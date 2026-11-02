package pl.shop.order.dto.response;

public record StockResponse(
        Long productId,
        int quantity,
        boolean available
) {
}
