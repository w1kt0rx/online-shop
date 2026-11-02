package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrencyException extends OrderServiceException {
    public InvalidCurrencyException(String currency) {
        super("INVALID_CURRENCY", "Currency cannot be blank: currency = " + currency, HttpStatus.BAD_REQUEST);
    }
}
