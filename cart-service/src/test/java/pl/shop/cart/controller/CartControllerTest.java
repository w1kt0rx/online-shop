package pl.shop.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.cart.dto.request.AddCartItemRequest;
import pl.shop.cart.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.cart.dto.response.CartItemComponentResponse;
import pl.shop.cart.dto.response.CartItemResponse;
import pl.shop.cart.dto.response.CartResponse;
import pl.shop.cart.dto.response.CartTotalResponse;
import pl.shop.cart.dto.response.PriceResponse;
import pl.shop.cart.exception.CartAlreadyOrderedException;
import pl.shop.cart.exception.CartItemNotFoundException;
import pl.shop.cart.exception.CartNotFoundException;
import pl.shop.cart.exception.InsufficientStockException;
import pl.shop.cart.model.CartStatus;
import pl.shop.cart.service.CartService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CartService cartService;

    @Test
    void createCart_noBody_returnsCreatedEmptyCart() throws Exception {
        //given
        CartResponse response = new CartResponse(1L, CartStatus.ACTIVE, List.of(),
                new CartTotalResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN"));
        when(cartService.createCart()).thenReturn(response);
        //when //then
        mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total.grossPrice").value(0))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
        Mockito.verify(cartService).createCart();
    }

    @Test
    void getCart_cartExists_returnsCart() throws Exception {
        //given
        PriceResponse unitPrice = new PriceResponse(new BigDecimal("5855.00"), new BigDecimal("0.23"),
                new BigDecimal("1346.65"), new BigDecimal("7201.65"), "PLN");
        CartItemResponse item = new CartItemResponse(100L, 1L, "Gaming PC Pro", 1, unitPrice, unitPrice,
                List.of(new CartItemComponentResponse(11L, 4L, "Graphics card", "RTX 4070")));
        CartResponse response = new CartResponse(1L, CartStatus.ACTIVE, List.of(item),
                new CartTotalResponse(new BigDecimal("5855.00"), new BigDecimal("1346.65"), new BigDecimal("7201.65"), "PLN"));
        ArgumentCaptor<Long> cartIdCaptor = ArgumentCaptor.forClass(Long.class);
        when(cartService.getCart(1L)).thenReturn(response);
        //when //then
        mockMvc.perform(get("/carts/{cartId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(100L))
                .andExpect(jsonPath("$.items[0].productName").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.items[0].unitPrice.grossPrice").value(7201.65))
                .andExpect(jsonPath("$.items[0].components[0].configurationId").value(11L))
                .andExpect(jsonPath("$.items[0].components[0].componentName").value("RTX 4070"))
                .andExpect(jsonPath("$.total.netPrice").value(5855.00));
        Mockito.verify(cartService).getCart(cartIdCaptor.capture());
        Assertions.assertEquals(1L, cartIdCaptor.getValue());
    }

    @Test
    void getCart_cartNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        when(cartService.getCart(999L)).thenThrow(new CartNotFoundException(999L));
        //when //then
        mockMvc.perform(get("/carts/{cartId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("CART_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cart with id = 999 doesn't exist"));
    }

    @Test
    void getCart_cartIdNotNumber_returnsBadRequest() throws Exception {
        //given
        String cartId = "abc";
        //when //then
        mockMvc.perform(get("/carts/{cartId}", cartId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message", Matchers.containsString("cartId")));
        Mockito.verify(cartService, Mockito.never()).getCart(anyLong());
    }

    @Test
    void addItem_dataCorrect_returnsUpdatedCart() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 2);
        PriceResponse unitPrice = new PriceResponse(new BigDecimal("5855.00"), new BigDecimal("0.23"),
                new BigDecimal("1346.65"), new BigDecimal("7201.65"), "PLN");
        PriceResponse totalPrice = new PriceResponse(new BigDecimal("11710.00"), new BigDecimal("0.23"),
                new BigDecimal("2693.30"), new BigDecimal("14403.30"), "PLN");
        CartItemResponse item = new CartItemResponse(100L, 1L, "Gaming PC Pro", 2, unitPrice, totalPrice,
                List.of(new CartItemComponentResponse(11L, 4L, "Graphics card", "RTX 4070")));
        CartResponse response = new CartResponse(1L, CartStatus.ACTIVE, List.of(item),
                new CartTotalResponse(new BigDecimal("11710.00"), new BigDecimal("2693.30"), new BigDecimal("14403.30"), "PLN"));
        ArgumentCaptor<Long> cartIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AddCartItemRequest> requestCaptor = ArgumentCaptor.forClass(AddCartItemRequest.class);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class))).thenReturn(response);
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(11710.00))
                .andExpect(jsonPath("$.total.grossPrice").value(14403.30));
        Mockito.verify(cartService).addItem(cartIdCaptor.capture(), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, cartIdCaptor.getValue()),
                () -> Assertions.assertEquals(1L, requestCaptor.getValue().productId()),
                () -> Assertions.assertEquals(List.of(11L), requestCaptor.getValue().configurationIds()),
                () -> Assertions.assertEquals(2, requestCaptor.getValue().quantity())
        );
    }

    @Test
    void addItem_configurationIdsMissing_emptyListPassedToService() throws Exception {
        //given
        String body = "{\"productId\":5,\"quantity\":1}";
        ArgumentCaptor<AddCartItemRequest> requestCaptor = ArgumentCaptor.forClass(AddCartItemRequest.class);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class))).thenReturn(new CartResponse(1L, CartStatus.ACTIVE,
                List.of(), new CartTotalResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN")));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        Mockito.verify(cartService).addItem(anyLong(), requestCaptor.capture());
        Assertions.assertTrue(requestCaptor.getValue().configurationIds().isEmpty());
    }

    @Test
    void addItem_productIdNull_returnsBadRequestWithMessage() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(null, List.of(), 1);
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Product ID is required")));
        Mockito.verify(cartService, Mockito.never()).addItem(anyLong(), any());
    }

    @Test
    void addItem_quantityAboveMax_returnsBadRequestWithMessage() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 100);
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Quantity cannot exceed 99")));
        Mockito.verify(cartService, Mockito.never()).addItem(anyLong(), any());
    }

    @Test
    void addItem_quantityZero_returnsBadRequestWithMessage() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 0);
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("Quantity must be at least 1")));
    }

    @Test
    void addItem_configurationIdNull_returnsBadRequestWithMessage() throws Exception {
        //given
        String body = "{\"productId\":1,\"configurationIds\":[11,null],\"quantity\":1}";
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Configuration ID cannot be null")));
        Mockito.verify(cartService, Mockito.never()).addItem(anyLong(), any());
    }

    @Test
    void addItem_bodyMalformed_returnsBadRequest() throws Exception {
        //given
        String body = "{\"productId\":";
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void addItem_insufficientStock_returnsConflictWithMessage() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 2);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new InsufficientStockException(4L, 2, 1));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 4. Required: 2, available: 1"));
    }

    @Test
    void addItem_cartOrdered_returnsConflict() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new CartAlreadyOrderedException(1L));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_ALREADY_ORDERED"));
    }

    @Test
    void addItem_productServiceReturnsNotFound_returnsNotFoundError() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(999L, List.of(), 1);
        Request feignRequest = Request.create(Request.HttpMethod.POST, "http://localhost:8081/products/999/configurations/quote",
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new FeignException.NotFound("Not Found", feignRequest, null, null));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REMOTE_SERVICE_ERROR"));
    }

    @Test
    void addItem_remoteServiceUnavailable_returnsServiceUnavailable() throws Exception {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(), 1);
        Request feignRequest = Request.create(Request.HttpMethod.POST, "http://localhost:8081/products/5/configurations/quote",
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(cartService.addItem(anyLong(), any(AddCartItemRequest.class)))
                .thenThrow(new RetryableException(503, "Service Unavailable", Request.HttpMethod.POST, (Long) null, feignRequest));
        //when //then
        mockMvc.perform(post("/carts/{cartId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void changeItemQuantity_dataCorrect_returnsUpdatedCart() throws Exception {
        //given
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(3);
        PriceResponse unitPrice = new PriceResponse(new BigDecimal("240.00"), new BigDecimal("0.23"),
                new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN");
        PriceResponse totalPrice = new PriceResponse(new BigDecimal("720.00"), new BigDecimal("0.23"),
                new BigDecimal("165.60"), new BigDecimal("885.60"), "PLN");
        CartResponse response = new CartResponse(1L, CartStatus.ACTIVE,
                List.of(new CartItemResponse(100L, 5L, "Headphones ABC", 3, unitPrice, totalPrice, List.of())),
                new CartTotalResponse(new BigDecimal("720.00"), new BigDecimal("165.60"), new BigDecimal("885.60"), "PLN"));
        ArgumentCaptor<Long> cartIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> itemIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ChangeCartItemQuantityRequest> requestCaptor = ArgumentCaptor.forClass(ChangeCartItemQuantityRequest.class);
        when(cartService.changeItemQuantity(anyLong(), anyLong(), any(ChangeCartItemQuantityRequest.class))).thenReturn(response);
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", 1L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total.grossPrice").value(885.60));
        Mockito.verify(cartService).changeItemQuantity(cartIdCaptor.capture(), itemIdCaptor.capture(), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, cartIdCaptor.getValue()),
                () -> Assertions.assertEquals(100L, itemIdCaptor.getValue()),
                () -> Assertions.assertEquals(3, requestCaptor.getValue().quantity())
        );
    }

    @Test
    void changeItemQuantity_quantityNull_returnsBadRequestWithMessage() throws Exception {
        //given
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(null);
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", 1L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Quantity is required")));
        Mockito.verify(cartService, Mockito.never()).changeItemQuantity(anyLong(), anyLong(), any());
    }

    @Test
    void changeItemQuantity_itemNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(2);
        when(cartService.changeItemQuantity(anyLong(), anyLong(), any(ChangeCartItemQuantityRequest.class)))
                .thenThrow(new CartItemNotFoundException(1L, 999L));
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", 1L, 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Item with id = 999 not found in cart with id = 1"));
    }

    @Test
    void changeItemQuantity_insufficientStock_returnsConflict() throws Exception {
        //given
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(10);
        when(cartService.changeItemQuantity(anyLong(), anyLong(), any(ChangeCartItemQuantityRequest.class)))
                .thenThrow(new InsufficientStockException(5L, 10, 3));
        //when //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", 1L, 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void removeItem_itemExists_returnsUpdatedCart() throws Exception {
        //given
        CartResponse response = new CartResponse(1L, CartStatus.ACTIVE, List.of(),
                new CartTotalResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN"));
        ArgumentCaptor<Long> cartIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> itemIdCaptor = ArgumentCaptor.forClass(Long.class);
        when(cartService.removeItem(anyLong(), anyLong())).thenReturn(response);
        //when //then
        mockMvc.perform(delete("/carts/{cartId}/items/{itemId}", 1L, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
        Mockito.verify(cartService).removeItem(cartIdCaptor.capture(), itemIdCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, cartIdCaptor.getValue()),
                () -> Assertions.assertEquals(100L, itemIdCaptor.getValue())
        );
    }

    @Test
    void removeItem_itemNotExists_returnsNotFound() throws Exception {
        //given
        when(cartService.removeItem(1L, 999L)).thenThrow(new CartItemNotFoundException(1L, 999L));
        //when //then
        mockMvc.perform(delete("/carts/{cartId}/items/{itemId}", 1L, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void removeItem_cartOrdered_returnsConflict() throws Exception {
        //given
        when(cartService.removeItem(1L, 100L)).thenThrow(new CartAlreadyOrderedException(1L));
        //when //then
        mockMvc.perform(delete("/carts/{cartId}/items/{itemId}", 1L, 100L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_ALREADY_ORDERED"));
    }
}
