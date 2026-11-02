package pl.shop.cart.dto.response;

import pl.shop.cart.model.CartStatus;

import java.util.List;

public record CartResponse(
        Long id,
        CartStatus status,
        List<CartItemResponse> items,
        CartTotalResponse total
) {
}
