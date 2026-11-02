package pl.shop.inventory.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InventoryServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public InventoryServiceException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
