package pl.shop.interaction.dto.request;

public record CreateProductRequest(
        String name,
        String category,
        String vendor,
        PriceRequest price,
        Boolean sellableSeparately
) {
}
