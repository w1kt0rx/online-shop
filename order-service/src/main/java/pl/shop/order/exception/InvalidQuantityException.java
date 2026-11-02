package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

public class InvalidQuantityException extends OrderServiceException {
    public InvalidQuantityException(int quantity) {
        super("INVALID_QUANTITY", "Quantity must be at least 1. Given: " + quantity, HttpStatus.BAD_REQUEST);
    }
}
