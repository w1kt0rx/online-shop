package pl.shop.order.dto.response;

import java.util.List;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        PriceResponse unitPrice,
        PriceResponse totalPrice,
        List<CartItemComponentResponse> components
) {
}
