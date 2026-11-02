package pl.shop.cart.dto.response;

public record StockResponse(
        Long productId,
        int quantity,
        boolean available
) {
}
