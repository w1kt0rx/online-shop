package pl.shop.interaction.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long cartId,
        String status,
        CustomerResponse customer,
        AddressResponse shippingAddress,
        List<OrderItemResponse> items,
        OrderTotalResponse total,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
}
