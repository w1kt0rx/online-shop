package pl.shop.cart.exception;

import org.springframework.http.HttpStatus;

public class InvalidQuantityException extends CartServiceException {
    public InvalidQuantityException(int quantity, int max) {
        super("INVALID_QUANTITY", "Quantity must be between 1 and " + max + ". Given: " + quantity, HttpStatus.BAD_REQUEST);
    }
}
