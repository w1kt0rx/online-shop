package pl.shop.interaction.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.shop.interaction.client.InvoiceServiceClient;
import pl.shop.interaction.client.OrderServiceClient;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.PlaceOrderRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.OrderDetailsResponse;
import pl.shop.interaction.dto.response.OrderResponse;
import pl.shop.interaction.dto.response.OrderSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderServiceClient orderServiceClient;
    private final InvoiceServiceClient invoiceServiceClient;

    public OrderResponse placeOrder(PlaceOrderRequest request) {
        log.debug("Placing order for cart with id = {}", request.cartId());
        OrderResponse order = orderServiceClient.placeOrder(request);
        log.info("Order {} placed for cart with id = {}", order.orderNumber(), request.cartId());
        return order;
    }

    public OrderDetailsResponse getOrderDetails(Long orderId) {
        log.debug("Fetching details of order with id = {}", orderId);
        OrderResponse order = orderServiceClient.getOrder(orderId);
        InvoiceResponse invoice = findInvoice(orderId);
        log.info("Details of order with id = {} fetched, invoice present = {}", orderId, invoice != null);
        return new OrderDetailsResponse(order, invoice);
    }

    public PageResponse<OrderSummaryResponse> getOrders(String email, PaginationRequest pagination) {
        log.debug("Fetching orders, email = {}", email);
        return orderServiceClient.getOrders(email, pagination.page(), pagination.size(),
                pagination.sortBy(), pagination.direction());
    }

    public OrderResponse cancelOrder(Long orderId) {
        log.debug("Cancelling order with id = {}", orderId);
        return orderServiceClient.cancelOrder(orderId);
    }

    private InvoiceResponse findInvoice(Long orderId) {
        try {
            return invoiceServiceClient.getInvoiceByOrder(orderId);
        } catch (FeignException.NotFound ex) {
            log.info("Invoice for order with id = {} not issued yet", orderId);
            return null;
        }
    }
}
