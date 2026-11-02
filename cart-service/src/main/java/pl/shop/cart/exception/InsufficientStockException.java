package pl.shop.cart.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends CartServiceException {
    public InsufficientStockException(Long productId, int required, int available) {
        super("INSUFFICIENT_STOCK", "Insufficient stock for product with id = " + productId
                + ". Required: " + required + ", available: " + available, HttpStatus.CONFLICT);
    }
}
