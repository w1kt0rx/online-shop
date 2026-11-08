package pl.shop.interaction.dto.response;

public record ProductDetailsResponse(
        ProductResponse product,
        StockResponse stock
) {
}
