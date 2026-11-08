package pl.shop.interaction.dto.response;

public record ProductSummaryResponse(
        Long id,
        String name,
        String category,
        String vendor,
        PriceResponse price,
        boolean configurable
) {
}
