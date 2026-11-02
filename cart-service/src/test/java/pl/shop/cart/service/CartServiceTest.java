package pl.shop.cart.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.cart.client.InventoryServiceClient;
import pl.shop.cart.client.ProductServiceClient;
import pl.shop.cart.dto.request.AddCartItemRequest;
import pl.shop.cart.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.cart.dto.request.QuoteRequest;
import pl.shop.cart.dto.response.CartResponse;
import pl.shop.cart.dto.response.PriceResponse;
import pl.shop.cart.dto.response.QuoteComponentResponse;
import pl.shop.cart.dto.response.QuoteResponse;
import pl.shop.cart.dto.response.StockResponse;
import pl.shop.cart.exception.CartAlreadyOrderedException;
import pl.shop.cart.exception.CartItemNotFoundException;
import pl.shop.cart.exception.CartNotFoundException;
import pl.shop.cart.exception.InsufficientStockException;
import pl.shop.cart.mapper.CartMapper;
import pl.shop.cart.mapper.PriceMapper;
import pl.shop.cart.model.Cart;
import pl.shop.cart.model.CartItem;
import pl.shop.cart.model.CartItemComponent;
import pl.shop.cart.model.CartStatus;
import pl.shop.cart.model.Price;
import pl.shop.cart.repository.CartRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class CartServiceTest {
    private CartService cartService;
    private CartRepository cartRepository;
    private ProductServiceClient productServiceClient;
    private InventoryServiceClient inventoryServiceClient;

    @BeforeEach
    void setup() {
        this.cartRepository = Mockito.mock(CartRepository.class);
        this.productServiceClient = Mockito.mock(ProductServiceClient.class);
        this.inventoryServiceClient = Mockito.mock(InventoryServiceClient.class);
        PriceMapper priceMapper = Mappers.getMapper(PriceMapper.class);
        CartMapper cartMapper = Mappers.getMapper(CartMapper.class);
        ReflectionTestUtils.setField(cartMapper, "priceMapper", priceMapper);
        this.cartService = new CartService(cartRepository, cartMapper, priceMapper, productServiceClient, inventoryServiceClient);
    }

    @Test
    void createCart_noArguments_activeEmptyCartSavedAndReturned() {
        //given
        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            ReflectionTestUtils.setField(cart, "id", 1L);
            return cart;
        });
        //when
        CartResponse response = cartService.createCart();
        //then
        Mockito.verify(cartRepository).save(cartCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals(CartStatus.ACTIVE, response.status()),
                () -> Assertions.assertTrue(response.items().isEmpty()),
                () -> Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(response.total().grossPrice())),
                () -> Assertions.assertEquals("PLN", response.total().currency()),
                () -> Assertions.assertEquals(CartStatus.ACTIVE, cartCaptor.getValue().getStatus())
        );
    }

    @Test
    void getCart_cartExists_returnsCartWithItemsAndTotals() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem pc = cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 1,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        ReflectionTestUtils.setField(pc, "id", 100L);
        CartItem headphones = cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 2, List.of());
        ReflectionTestUtils.setField(headphones, "id", 101L);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when
        CartResponse response = cartService.getCart(1L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals(2, response.items().size()),
                () -> Assertions.assertEquals(100L, response.items().getFirst().id()),
                () -> Assertions.assertEquals("Gaming PC Pro", response.items().getFirst().productName()),
                () -> Assertions.assertEquals(1, response.items().getFirst().components().size()),
                () -> Assertions.assertEquals(11L, response.items().getFirst().components().getFirst().configurationId()),
                () -> Assertions.assertEquals(4L, response.items().getFirst().components().getFirst().componentProductId()),
                () -> Assertions.assertEquals("RTX 4070", response.items().getFirst().components().getFirst().componentName()),
                () -> Assertions.assertEquals(new BigDecimal("7201.65"), response.items().getFirst().unitPrice().grossPrice()),
                () -> Assertions.assertEquals(2, response.items().get(1).quantity()),
                () -> Assertions.assertEquals(new BigDecimal("480.00"), response.items().get(1).totalPrice().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.items().get(1).totalPrice().grossPrice()),
                () -> Assertions.assertEquals(new BigDecimal("6335.00"), response.total().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("1457.05"), response.total().vatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("7792.05"), response.total().grossPrice())
        );
    }

    @Test
    void getCart_cartNotExists_throwsCartNotFoundException() {
        //given
        when(cartRepository.findById(999L)).thenReturn(Optional.empty());
        //when //then
        CartNotFoundException ex = Assertions.assertThrows(
                CartNotFoundException.class,
                () -> cartService.getCart(999L)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("CART_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals("Cart with id = 999 doesn't exist", ex.getMessage())
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void addItem_stockSufficient_itemAddedWithQuotedPriceAndComponents() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 2);
        QuoteResponse quote = new QuoteResponse(1L, "Gaming PC Pro",
                List.of(new QuoteComponentResponse(11L, 4L, "Graphics card", "RTX 4070")),
                new PriceResponse(new BigDecimal("5855.00"), new BigDecimal("0.23"), new BigDecimal("1346.65"),
                        new BigDecimal("7201.65"), "PLN"),
                List.of(1L, 4L));
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<QuoteRequest> quoteRequestCaptor = ArgumentCaptor.forClass(QuoteRequest.class);
        ArgumentCaptor<List<Long>> stockIdsCaptor = ArgumentCaptor.forClass(List.class);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(
                new StockResponse(1L, 5, true),
                new StockResponse(4L, 2, true)
        ));
        //when
        CartResponse response = cartService.addItem(1L, request);
        //then
        Mockito.verify(productServiceClient).quote(productIdCaptor.capture(), quoteRequestCaptor.capture());
        Mockito.verify(inventoryServiceClient).getStock(stockIdsCaptor.capture());
        Mockito.verify(cartRepository).flush();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, productIdCaptor.getValue()),
                () -> Assertions.assertEquals(List.of(11L), quoteRequestCaptor.getValue().configurationIds()),
                () -> Assertions.assertEquals(List.of(1L, 4L), stockIdsCaptor.getValue()),
                () -> Assertions.assertEquals(1, response.items().size()),
                () -> Assertions.assertEquals(1L, response.items().getFirst().productId()),
                () -> Assertions.assertEquals("Gaming PC Pro", response.items().getFirst().productName()),
                () -> Assertions.assertEquals(2, response.items().getFirst().quantity()),
                () -> Assertions.assertEquals(new BigDecimal("5855.00"), response.items().getFirst().unitPrice().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("11710.00"), response.items().getFirst().totalPrice().netPrice()),
                () -> Assertions.assertEquals("RTX 4070", response.items().getFirst().components().getFirst().componentName()),
                () -> Assertions.assertEquals(new BigDecimal("11710.00"), response.total().netPrice()),
                () -> Assertions.assertEquals(1, cart.getItems().size())
        );
    }

    @Test
    void addItem_sameProductAlreadyInCart_quantityMergedAndWholeQuantityChecked() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem existing = cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 2, List.of());
        ReflectionTestUtils.setField(existing, "id", 100L);
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        QuoteResponse quote = new QuoteResponse(5L, "Headphones ABC", List.of(),
                new PriceResponse(new BigDecimal("240.00"), new BigDecimal("0.23"), new BigDecimal("55.20"),
                        new BigDecimal("295.20"), "PLN"),
                List.of(5L));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(new StockResponse(5L, 3, true)));
        //when
        CartResponse response = cartService.addItem(1L, request);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, response.items().size()),
                () -> Assertions.assertEquals(100L, response.items().getFirst().id()),
                () -> Assertions.assertEquals(3, response.items().getFirst().quantity())
        );
    }

    @Test
    void addItem_componentStockInsufficient_throwsInsufficientStockExceptionAndNothingAdded() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 2);
        QuoteResponse quote = new QuoteResponse(1L, "Gaming PC Pro",
                List.of(new QuoteComponentResponse(11L, 4L, "Graphics card", "RTX 4070")),
                new PriceResponse(new BigDecimal("5855.00"), new BigDecimal("0.23"), new BigDecimal("1346.65"),
                        new BigDecimal("7201.65"), "PLN"),
                List.of(1L, 4L));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(
                new StockResponse(1L, 5, true),
                new StockResponse(4L, 1, true)
        ));
        //when //then
        InsufficientStockException ex = Assertions.assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(1L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INSUFFICIENT_STOCK", ex.getErrorCode()),
                () -> Assertions.assertEquals("Insufficient stock for product with id = 4. Required: 2, available: 1", ex.getMessage()),
                () -> Assertions.assertTrue(cart.getItems().isEmpty())
        );
        Mockito.verify(cartRepository, Mockito.never()).flush();
    }

    @Test
    void addItem_componentAlreadyUsedByOtherItem_requirementsSummedAndInsufficientStockThrown() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem pc = cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        ReflectionTestUtils.setField(pc, "id", 100L);
        AddCartItemRequest request = new AddCartItemRequest(4L, List.of(), 1);
        QuoteResponse quote = new QuoteResponse(4L, "RTX 4070", List.of(),
                new PriceResponse(new BigDecimal("2275.00"), new BigDecimal("0.23"), new BigDecimal("523.25"),
                        new BigDecimal("2798.25"), "PLN"),
                List.of(4L));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(
                new StockResponse(1L, 10, true),
                new StockResponse(4L, 2, true)
        ));
        //when //then
        InsufficientStockException ex = Assertions.assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(1L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Insufficient stock for product with id = 4. Required: 3, available: 2", ex.getMessage()),
                () -> Assertions.assertEquals(1, cart.getItems().size())
        );
    }

    @Test
    void addItem_productMissingInInventoryResponse_treatedAsZeroAndInsufficientStockThrown() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        QuoteResponse quote = new QuoteResponse(5L, "Headphones ABC", List.of(),
                new PriceResponse(new BigDecimal("240.00"), new BigDecimal("0.23"), new BigDecimal("55.20"),
                        new BigDecimal("295.20"), "PLN"),
                List.of(5L));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of());
        //when //then
        InsufficientStockException ex = Assertions.assertThrows(
                InsufficientStockException.class,
                () -> cartService.addItem(1L, request)
        );
        Assertions.assertEquals("Insufficient stock for product with id = 5. Required: 1, available: 0", ex.getMessage());
    }

    @Test
    void addItem_cartNotExists_throwsCartNotFoundExceptionAndRemoteServicesNotCalled() {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        when(cartRepository.findById(999L)).thenReturn(Optional.empty());
        //when //then
        Assertions.assertThrows(
                CartNotFoundException.class,
                () -> cartService.addItem(999L, request)
        );
        Mockito.verifyNoInteractions(productServiceClient, inventoryServiceClient);
    }

    @Test
    void addItem_cartOrdered_throwsCartAlreadyOrderedExceptionAndRemoteServicesNotCalled() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        ReflectionTestUtils.setField(cart, "status", CartStatus.ORDERED);
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when //then
        CartAlreadyOrderedException ex = Assertions.assertThrows(
                CartAlreadyOrderedException.class,
                () -> cartService.addItem(1L, request)
        );
        Assertions.assertEquals("CART_ALREADY_ORDERED", ex.getErrorCode());
        Mockito.verifyNoInteractions(productServiceClient, inventoryServiceClient);
    }

    @Test
    void addItem_productServiceReturnsNotFound_feignExceptionPropagatedAndInventoryNotCalled() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        AddCartItemRequest request = new AddCartItemRequest(999L, List.of(), 1);
        Request feignRequest = Request.create(Request.HttpMethod.POST, "http://localhost:8081/products/999/configurations/quote",
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class)))
                .thenThrow(new FeignException.NotFound("Not Found", feignRequest, null, null));
        //when //then
        FeignException ex = Assertions.assertThrows(
                FeignException.class,
                () -> cartService.addItem(1L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals(404, ex.status()),
                () -> Assertions.assertTrue(cart.getItems().isEmpty())
        );
        Mockito.verifyNoInteractions(inventoryServiceClient);
    }
    @Test
    void addItem_quoteBeforeStockCheck_callsInOrder() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        QuoteResponse quote = new QuoteResponse(5L, "Headphones ABC", List.of(),
                new PriceResponse(new BigDecimal("240.00"), new BigDecimal("0.23"), new BigDecimal("55.20"),
                        new BigDecimal("295.20"), "PLN"),
                List.of(5L));
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(new StockResponse(5L, 1, true)));
        //when
        cartService.addItem(1L, request);
        //then
        InOrder inOrder = Mockito.inOrder(productServiceClient, inventoryServiceClient, cartRepository);
        inOrder.verify(productServiceClient).quote(anyLong(), any(QuoteRequest.class));
        inOrder.verify(inventoryServiceClient).getStock(any());
        inOrder.verify(cartRepository).flush();
    }

    @Test
    @SuppressWarnings("unchecked")
    void changeItemQuantity_increaseWithSufficientStock_quantityChanged() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem pc = cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 1,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        ReflectionTestUtils.setField(pc, "id", 100L);
        ArgumentCaptor<List<Long>> stockIdsCaptor = ArgumentCaptor.forClass(List.class);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(
                new StockResponse(1L, 3, true),
                new StockResponse(4L, 3, true)
        ));
        //when
        CartResponse response = cartService.changeItemQuantity(1L, 100L, new ChangeCartItemQuantityRequest(3));
        //then
        Mockito.verify(inventoryServiceClient).getStock(stockIdsCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(1L, 4L), stockIdsCaptor.getValue()),
                () -> Assertions.assertEquals(3, response.items().getFirst().quantity()),
                () -> Assertions.assertEquals(3, pc.getQuantity())
        );
    }

    @Test
    void changeItemQuantity_increaseWithInsufficientStock_throwsInsufficientStockExceptionAndQuantityUnchanged() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem pc = cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 1,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        ReflectionTestUtils.setField(pc, "id", 100L);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(inventoryServiceClient.getStock(any())).thenReturn(List.of(
                new StockResponse(1L, 10, true),
                new StockResponse(4L, 2, true)
        ));
        //when //then
        InsufficientStockException ex = Assertions.assertThrows(
                InsufficientStockException.class,
                () -> cartService.changeItemQuantity(1L, 100L, new ChangeCartItemQuantityRequest(3))
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Insufficient stock for product with id = 4. Required: 3, available: 2", ex.getMessage()),
                () -> Assertions.assertEquals(1, pc.getQuantity())
        );
    }

    @Test
    void changeItemQuantity_decrease_inventoryNotCalledAndQuantityChanged() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem headphones = cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 4, List.of());
        ReflectionTestUtils.setField(headphones, "id", 100L);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when
        CartResponse response = cartService.changeItemQuantity(1L, 100L, new ChangeCartItemQuantityRequest(1));
        //then
        Mockito.verifyNoInteractions(inventoryServiceClient);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, response.items().getFirst().quantity()),
                () -> Assertions.assertEquals(new BigDecimal("240.00"), response.total().netPrice())
        );
    }

    @Test
    void changeItemQuantity_itemNotExists_throwsCartItemNotFoundException() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when //then
        CartItemNotFoundException ex = Assertions.assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.changeItemQuantity(1L, 999L, new ChangeCartItemQuantityRequest(2))
        );
        Assertions.assertEquals("Item with id = 999 not found in cart with id = 1", ex.getMessage());
        Mockito.verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void changeItemQuantity_cartOrdered_throwsCartAlreadyOrderedException() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem headphones = cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        ReflectionTestUtils.setField(headphones, "id", 100L);
        ReflectionTestUtils.setField(cart, "status", CartStatus.ORDERED);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when //then
        Assertions.assertThrows(
                CartAlreadyOrderedException.class,
                () -> cartService.changeItemQuantity(1L, 100L, new ChangeCartItemQuantityRequest(2))
        );
        Assertions.assertEquals(1, headphones.getQuantity());
    }

    @Test
    void removeItem_itemExists_itemRemovedAndFlushed() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem headphones = cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        ReflectionTestUtils.setField(headphones, "id", 100L);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when
        CartResponse response = cartService.removeItem(1L, 100L);
        //then
        Mockito.verify(cartRepository).flush();
        Assertions.assertAll(
                () -> Assertions.assertTrue(response.items().isEmpty()),
                () -> Assertions.assertTrue(cart.getItems().isEmpty())
        );
    }

    @Test
    void removeItem_itemNotExists_throwsCartItemNotFoundException() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        //when //then
        Assertions.assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.removeItem(1L, 999L)
        );
        Mockito.verify(cartRepository, Mockito.never()).flush();
    }

    @Test
    void removeItem_cartNotExists_throwsCartNotFoundException() {
        //given
        when(cartRepository.findById(999L)).thenReturn(Optional.empty());
        //when //then
        Assertions.assertThrows(
                CartNotFoundException.class,
                () -> cartService.removeItem(999L, 100L)
        );
    }
}
