package pl.shop.product.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ProductServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public ProductServiceException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
