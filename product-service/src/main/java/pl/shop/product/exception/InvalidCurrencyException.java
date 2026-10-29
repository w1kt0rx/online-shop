package pl.shop.product.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrencyException extends ProductServiceException {
    public InvalidCurrencyException(String currency) {
        super("INVALID_CURRENCY", "Currency cannot be blank: currency = " + currency, HttpStatus.BAD_REQUEST);
    }
}
