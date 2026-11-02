package pl.shop.cart.dto.response;

import java.util.List;

public record QuoteResponse(
        Long productId,
        String productName,
        List<QuoteComponentResponse> selectedComponents,
        PriceResponse unitPrice,
        List<Long> stockProductIds
) {
}
