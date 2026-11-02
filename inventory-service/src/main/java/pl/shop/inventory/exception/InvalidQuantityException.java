package pl.shop.inventory.exception;

import org.springframework.http.HttpStatus;

public class InvalidQuantityException extends InventoryServiceException {
    public InvalidQuantityException(String message) {
        super("INVALID_QUANTITY", message, HttpStatus.BAD_REQUEST);
    }
}
