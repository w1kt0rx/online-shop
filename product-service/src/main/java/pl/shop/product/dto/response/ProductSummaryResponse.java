package pl.shop.product.dto.response;

public record ProductSummaryResponse(
        Long id,
        String name,
        String category,
        String vendor,
        PriceResponse price,
        boolean configurable
) {
}
