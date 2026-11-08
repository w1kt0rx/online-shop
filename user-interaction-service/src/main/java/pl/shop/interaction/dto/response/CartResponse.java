package pl.shop.interaction.dto.response;

import java.util.List;

public record CartResponse(
        Long id,
        String status,
        List<CartItemResponse> items,
        CartTotalResponse total
) {
}
