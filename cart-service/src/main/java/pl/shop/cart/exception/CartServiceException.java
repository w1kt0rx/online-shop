package pl.shop.cart.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CartServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public CartServiceException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
