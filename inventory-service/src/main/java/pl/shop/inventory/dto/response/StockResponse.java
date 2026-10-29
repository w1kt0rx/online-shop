package pl.shop.inventory.dto.response;

public record StockResponse(
        Long productId,
        int quantity,
        boolean available
) {
}
