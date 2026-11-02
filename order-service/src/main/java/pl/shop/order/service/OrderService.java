package pl.shop.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.order.client.CartServiceClient;
import pl.shop.order.client.InventoryServiceClient;
import pl.shop.order.dto.request.PaginationRequest;
import pl.shop.order.dto.request.PlaceOrderRequest;
import pl.shop.order.dto.request.StockReservationItemRequest;
import pl.shop.order.dto.request.StockReservationRequest;
import pl.shop.order.dto.response.*;
import pl.shop.order.exception.CartEmptyException;
import pl.shop.order.exception.CartNotActiveException;
import pl.shop.order.exception.OrderAlreadyExistsException;
import pl.shop.order.exception.OrderNotFoundException;
import pl.shop.order.kafka.KafkaSender;
import pl.shop.order.mapper.AddressMapper;
import pl.shop.order.mapper.CustomerMapper;
import pl.shop.order.mapper.OrderMapper;
import pl.shop.order.mapper.PriceMapper;
import pl.shop.order.model.CartStatus;
import pl.shop.order.model.Order;
import pl.shop.order.repository.OrderRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartServiceClient cartServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final CustomerMapper customerMapper;
    private final AddressMapper addressMapper;
    private final PriceMapper priceMapper;
    private final KafkaSender kafkaSender;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        log.debug("Attempting to place order for cart with id = {}", request.cartId());
        validateNoOrderForCart(request.cartId());
        CartResponse cartResponse = cartServiceClient.getCart(request.cartId());
        validateCart(cartResponse);
        Order order = createOrder(cartResponse, request);
        orderRepository.save(order);
        orderRepository.flush();
        inventoryServiceClient.reserve(toStockRequest(order));
        kafkaSender.sendOrderPlaced(order);
        log.info("Order with id = {} and number = {} placed for cart with id = {}", order.getId(), order.getOrderNumber(), order.getCartId());
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        log.debug("Attempting to fetch order with id = {}", orderId);
        Order order = fetchOrder(orderId);
        log.info("Successfully fetched order with id = {}", order.getId());
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrders(String email, PaginationRequest paginationRequest) {
        log.debug("Attempting to fetch orders, email = {}", email);
        Page<Order> orders = email == null || email.isBlank()
                ? orderRepository.findAll(paginationRequest.toPageable())
                : orderRepository.findByCustomerEmailIgnoreCase(email, paginationRequest.toPageable());
        log.info("Fetched {} orders", orders.getNumberOfElements());
        return PageResponse.from(orders.map(orderMapper::toSummaryResponse));
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        log.debug("Attempting to cancel order with id = {}", orderId);
        Order order = fetchOrder(orderId);
        order.cancel();
        orderRepository.flush();
        inventoryServiceClient.release(toStockRequest(order));
        kafkaSender.sendOrderCancelled(order);
        log.info("Successfully canceled order with id = {}", orderId);
        return orderMapper.toResponse(order);
    }

    private StockReservationRequest toStockRequest(Order order) {
        List<StockReservationItemRequest> items = order.stockRequirements()
                .entrySet()
                .stream()
                .map(entry -> new StockReservationItemRequest(entry.getKey(), entry.getValue()))
                .toList();
        return new StockReservationRequest(order.getOrderNumber(), items);
    }

    private Order createOrder(CartResponse cartResponse, PlaceOrderRequest request) {
        Order order = Order.place(cartResponse.id(),
                customerMapper.toEntity(request.customer()),
                addressMapper.toEntity(request.shippingAddress())
        );
        cartResponse.items()
                .forEach(item -> addItem(order, item));
        return order;
    }

    private void addItem(Order order, CartItemResponse item) {
        order.addItem(
                item.productId(),
                item.productName(),
                priceMapper.toEntity(item.unitPrice()),
                item.quantity(),
                orderMapper.toComponents(item.components())
        );
    }

    private void validateNoOrderForCart(Long cartId) {
        if (orderRepository.existsByCartId(cartId)) {
            throw new OrderAlreadyExistsException(cartId);
        }
    }

    private void validateCart(CartResponse cartResponse) {
        if (cartResponse.status() != CartStatus.ACTIVE) {
            throw new CartNotActiveException(cartResponse.id());
        }
        if (cartResponse.items().isEmpty()) {
            throw new CartEmptyException(cartResponse.id());
        }
    }

    private Order fetchOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(
                        () -> new OrderNotFoundException(orderId)
                );
    }
}
