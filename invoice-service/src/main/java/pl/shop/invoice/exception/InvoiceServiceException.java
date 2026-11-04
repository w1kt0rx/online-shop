package pl.shop.invoice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvoiceServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public InvoiceServiceException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
