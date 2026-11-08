package pl.shop.interaction.dto.request;

public record UpdateProductRequest(
        String name,
        String category,
        String vendor,
        PriceRequest price,
        Boolean sellableSeparately
) {
}
