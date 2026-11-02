package pl.shop.order.dto.response;

import java.util.List;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        PriceResponse unitPrice,
        PriceResponse totalPrice,
        List<OrderItemComponentResponse> components
) {
}
