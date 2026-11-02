package pl.shop.order.dto.response;

import pl.shop.order.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long cartId,
        OrderStatus status,
        CustomerResponse customer,
        AddressResponse shippingAddress,
        List<OrderItemResponse> items,
        OrderTotalResponse total,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
}
