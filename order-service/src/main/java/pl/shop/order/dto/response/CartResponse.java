package pl.shop.order.dto.response;

import pl.shop.order.model.CartStatus;

import java.util.List;

public record CartResponse(
        Long id,
        CartStatus status,
        List<CartItemResponse> items,
        CartTotalResponse total
) {
}
