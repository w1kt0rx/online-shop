package pl.shop.order.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.order.client.CartServiceClient;
import pl.shop.order.client.InventoryServiceClient;
import pl.shop.order.dto.request.AddressRequest;
import pl.shop.order.dto.request.CustomerRequest;
import pl.shop.order.dto.request.PaginationRequest;
import pl.shop.order.dto.request.PlaceOrderRequest;
import pl.shop.order.dto.request.StockReservationRequest;
import pl.shop.order.dto.response.CartItemComponentResponse;
import pl.shop.order.dto.response.CartItemResponse;
import pl.shop.order.dto.response.CartResponse;
import pl.shop.order.dto.response.CartTotalResponse;
import pl.shop.order.dto.response.OrderResponse;
import pl.shop.order.dto.response.OrderSummaryResponse;
import pl.shop.order.dto.response.PageResponse;
import pl.shop.order.dto.response.PriceResponse;
import pl.shop.order.exception.CartEmptyException;
import pl.shop.order.exception.CartNotActiveException;
import pl.shop.order.exception.InvalidOrderStatusException;
import pl.shop.order.exception.OrderAlreadyExistsException;
import pl.shop.order.exception.OrderNotFoundException;
import pl.shop.order.kafka.KafkaSender;
import pl.shop.order.mapper.AddressMapper;
import pl.shop.order.mapper.CustomerMapper;
import pl.shop.order.mapper.OrderMapper;
import pl.shop.order.mapper.PriceMapper;
import pl.shop.order.model.Address;
import pl.shop.order.model.CartStatus;
import pl.shop.order.model.Customer;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItemComponent;
import pl.shop.order.model.OrderStatus;
import pl.shop.order.model.Price;
import pl.shop.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OrderServiceTest {
    private OrderService orderService;
    private OrderRepository orderRepository;
    private CartServiceClient cartServiceClient;
    private InventoryServiceClient inventoryServiceClient;
    private KafkaSender kafkaSender;

    @BeforeEach
    void setup() {
        this.orderRepository = Mockito.mock(OrderRepository.class);
        this.cartServiceClient = Mockito.mock(CartServiceClient.class);
        this.inventoryServiceClient = Mockito.mock(InventoryServiceClient.class);
        this.kafkaSender = Mockito.mock(KafkaSender.class);
        PriceMapper priceMapper = Mappers.getMapper(PriceMapper.class);
        CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);
        AddressMapper addressMapper = Mappers.getMapper(AddressMapper.class);
        OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);
        ReflectionTestUtils.setField(orderMapper, "priceMapper", priceMapper);
        ReflectionTestUtils.setField(orderMapper, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(orderMapper, "addressMapper", addressMapper);
        this.orderService = new OrderService(orderRepository, orderMapper, cartServiceClient, inventoryServiceClient,
                customerMapper, addressMapper, priceMapper, kafkaSender);
    }

    @Test
    void placeOrder_cartActive_orderSavedStockReservedAndEventSent() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        PriceResponse unitPrice = new PriceResponse(new BigDecimal("5855.00"), new BigDecimal("0.23"),
                new BigDecimal("1346.65"), new BigDecimal("7201.65"), "PLN");
        CartResponse cart = new CartResponse(1L, CartStatus.ACTIVE, List.of(
                new CartItemResponse(100L, 5L, "Gaming PC Pro", 2, unitPrice, unitPrice, List.of(
                        new CartItemComponentResponse(2L, 2L, "Graphics card", "RTX 4070"),
                        new CartItemComponentResponse(4L, 4L, "RAM", "RAM 32 GB DDR5")
                ))),
                new CartTotalResponse(new BigDecimal("11710.00"), new BigDecimal("2693.30"), new BigDecimal("14403.30"), "PLN"));
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<StockReservationRequest> reservationCaptor = ArgumentCaptor.forClass(StockReservationRequest.class);
        when(orderRepository.existsByCartId(1L)).thenReturn(false);
        when(cartServiceClient.getCart(1L)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 10L);
            return order;
        });
        //when
        OrderResponse response = orderService.placeOrder(request);
        //then
        InOrder inOrder = Mockito.inOrder(orderRepository, inventoryServiceClient, kafkaSender);
        inOrder.verify(orderRepository).save(orderCaptor.capture());
        inOrder.verify(orderRepository).flush();
        inOrder.verify(inventoryServiceClient).reserve(reservationCaptor.capture());
        inOrder.verify(kafkaSender).sendOrderPlaced(orderCaptor.getValue());
        StockReservationRequest reservation = reservationCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, response.id()),
                () -> Assertions.assertEquals(OrderStatus.PLACED, response.status()),
                () -> Assertions.assertEquals(1L, response.cartId()),
                () -> Assertions.assertEquals("Jan", response.customer().firstName()),
                () -> Assertions.assertEquals("jan@example.com", response.customer().email()),
                () -> Assertions.assertEquals("80-001", response.shippingAddress().postalCode()),
                () -> Assertions.assertEquals(1, response.items().size()),
                () -> Assertions.assertEquals(2, response.items().getFirst().quantity()),
                () -> Assertions.assertEquals(2, response.items().getFirst().components().size()),
                () -> Assertions.assertEquals(new BigDecimal("11710.00"), response.total().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("14403.30"), response.total().grossPrice()),
                () -> Assertions.assertEquals(orderCaptor.getValue().getOrderNumber(), reservation.orderNumber()),
                () -> Assertions.assertEquals(3, reservation.items().size()),
                () -> Assertions.assertEquals(5L, reservation.items().get(0).productId()),
                () -> Assertions.assertEquals(2, reservation.items().get(0).quantity()),
                () -> Assertions.assertEquals(2L, reservation.items().get(1).productId()),
                () -> Assertions.assertEquals(4L, reservation.items().get(2).productId())
        );
    }

    @Test
    void placeOrder_orderForCartExists_throwsOrderAlreadyExistsExceptionAndCartNotFetched() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        when(orderRepository.existsByCartId(1L)).thenReturn(true);
        //when //then
        OrderAlreadyExistsException ex = Assertions.assertThrows(
                OrderAlreadyExistsException.class,
                () -> orderService.placeOrder(request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("ORDER_ALREADY_EXISTS", ex.getErrorCode()),
                () -> Assertions.assertEquals("Order for cart with id = 1 already exists", ex.getMessage())
        );
        Mockito.verifyNoInteractions(cartServiceClient, inventoryServiceClient, kafkaSender);
        Mockito.verify(orderRepository, Mockito.never()).save(any());
    }

    @Test
    void placeOrder_cartOrdered_throwsCartNotActiveExceptionAndNothingSaved() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        CartResponse cart = new CartResponse(1L, CartStatus.ORDERED, List.of(),
                new CartTotalResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN"));
        when(orderRepository.existsByCartId(1L)).thenReturn(false);
        when(cartServiceClient.getCart(1L)).thenReturn(cart);
        //when //then
        CartNotActiveException ex = Assertions.assertThrows(
                CartNotActiveException.class,
                () -> orderService.placeOrder(request)
        );
        Assertions.assertEquals("Cart with id = 1 is not active", ex.getMessage());
        Mockito.verify(orderRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(inventoryServiceClient, kafkaSender);
    }

    @Test
    void placeOrder_cartEmpty_throwsCartEmptyExceptionAndNothingSaved() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        CartResponse cart = new CartResponse(1L, CartStatus.ACTIVE, List.of(),
                new CartTotalResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN"));
        when(orderRepository.existsByCartId(1L)).thenReturn(false);
        when(cartServiceClient.getCart(1L)).thenReturn(cart);
        //when //then
        CartEmptyException ex = Assertions.assertThrows(
                CartEmptyException.class,
                () -> orderService.placeOrder(request)
        );
        Assertions.assertEquals("CART_EMPTY", ex.getErrorCode());
        Mockito.verify(orderRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(inventoryServiceClient, kafkaSender);
    }

    @Test
    void placeOrder_inventoryReturnsConflict_exceptionPropagatedAndEventNotSent() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        PriceResponse unitPrice = new PriceResponse(new BigDecimal("240.00"), new BigDecimal("0.23"),
                new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN");
        CartResponse cart = new CartResponse(1L, CartStatus.ACTIVE, List.of(
                new CartItemResponse(100L, 6L, "Headphones ABC", 1, unitPrice, unitPrice, List.of())),
                new CartTotalResponse(new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN"));
        Request feignRequest = Request.create(Request.HttpMethod.POST, "http://localhost:8082/inventory/reservations",
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(orderRepository.existsByCartId(1L)).thenReturn(false);
        when(cartServiceClient.getCart(1L)).thenReturn(cart);
        when(inventoryServiceClient.reserve(any(StockReservationRequest.class)))
                .thenThrow(new FeignException.Conflict("Conflict", feignRequest, null, null));
        //when //then
        FeignException ex = Assertions.assertThrows(
                FeignException.class,
                () -> orderService.placeOrder(request)
        );
        Assertions.assertEquals(409, ex.status());
        Mockito.verifyNoInteractions(kafkaSender);
    }

    @Test
    void getOrder_orderExists_returnsOrderResponse() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.addItem(6L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 2, List.of());
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        //when
        OrderResponse response = orderService.getOrder(10L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, response.id()),
                () -> Assertions.assertEquals(order.getOrderNumber(), response.orderNumber()),
                () -> Assertions.assertEquals("Kowalski", response.customer().lastName()),
                () -> Assertions.assertEquals("Gdańsk", response.shippingAddress().city()),
                () -> Assertions.assertEquals(new BigDecimal("480.00"), response.items().getFirst().totalPrice().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.total().grossPrice()),
                () -> Assertions.assertEquals("PLN", response.total().currency())
        );
    }

    @Test
    void getOrder_orderNotExists_throwsOrderNotFoundException() {
        //given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        //when //then
        OrderNotFoundException ex = Assertions.assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrder(999L)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("ORDER_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals("Order with id = 999 doesn't exist", ex.getMessage())
        );
    }

    @Test
    void getOrders_emailGiven_findByCustomerEmailCalled() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.addItem(6L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        when(orderRepository.findByCustomerEmailIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1));
        //when
        PageResponse<OrderSummaryResponse> result = orderService.getOrders("JAN@example.com", new PaginationRequest(null, null, null, null));
        //then
        Mockito.verify(orderRepository).findByCustomerEmailIgnoreCase(emailCaptor.capture(), any(Pageable.class));
        Mockito.verify(orderRepository, Mockito.never()).findAll(any(Pageable.class));
        Assertions.assertAll(
                () -> Assertions.assertEquals("JAN@example.com", emailCaptor.getValue()),
                () -> Assertions.assertEquals(1, result.content().size()),
                () -> Assertions.assertEquals(10L, result.content().getFirst().id()),
                () -> Assertions.assertEquals(1, result.content().getFirst().itemCount()),
                () -> Assertions.assertEquals(new BigDecimal("295.20"), result.content().getFirst().grossPrice()),
                () -> Assertions.assertEquals(1L, result.totalElements())
        );
    }

    @Test
    void getOrders_emailBlank_findAllCalled() {
        //given
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        //when
        PageResponse<OrderSummaryResponse> result = orderService.getOrders(" ", new PaginationRequest(null, null, null, null));
        //then
        Mockito.verify(orderRepository).findAll(any(Pageable.class));
        Mockito.verify(orderRepository, Mockito.never()).findByCustomerEmailIgnoreCase(any(), any());
        Assertions.assertTrue(result.content().isEmpty());
    }

    @Test
    void cancelOrder_orderPlaced_statusCancelledStockReleasedAndEventSent() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("6042.00"), new BigDecimal("0.23"), "PLN"), 2,
                List.of(new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070")));
        ArgumentCaptor<StockReservationRequest> releaseCaptor = ArgumentCaptor.forClass(StockReservationRequest.class);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        //when
        OrderResponse response = orderService.cancelOrder(10L);
        //then
        InOrder inOrder = Mockito.inOrder(orderRepository, inventoryServiceClient, kafkaSender);
        inOrder.verify(orderRepository).flush();
        inOrder.verify(inventoryServiceClient).release(releaseCaptor.capture());
        inOrder.verify(kafkaSender).sendOrderCancelled(order);
        Assertions.assertAll(
                () -> Assertions.assertEquals(OrderStatus.CANCELLED, response.status()),
                () -> Assertions.assertNotNull(response.cancelledAt()),
                () -> Assertions.assertEquals(order.getOrderNumber(), releaseCaptor.getValue().orderNumber()),
                () -> Assertions.assertEquals(2, releaseCaptor.getValue().items().size()),
                () -> Assertions.assertEquals(2, releaseCaptor.getValue().items().get(1).quantity())
        );
    }

    @Test
    void cancelOrder_orderAlreadyCancelled_throwsInvalidOrderStatusExceptionAndNothingReleased() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.cancel();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        //when //then
        Assertions.assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.cancelOrder(10L)
        );
        Mockito.verify(orderRepository, Mockito.never()).flush();
        Mockito.verifyNoInteractions(inventoryServiceClient, kafkaSender);
    }

    @Test
    void cancelOrder_orderNotExists_throwsOrderNotFoundException() {
        //given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        //when //then
        Assertions.assertThrows(
                OrderNotFoundException.class,
                () -> orderService.cancelOrder(999L)
        );
        Mockito.verifyNoInteractions(inventoryServiceClient, kafkaSender);
    }
}
