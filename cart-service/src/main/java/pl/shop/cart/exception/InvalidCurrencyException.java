package pl.shop.cart.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrencyException extends CartServiceException {
    public InvalidCurrencyException(String currency) {
        super("INVALID_CURRENCY", "Currency cannot be blank: currency = " + currency, HttpStatus.BAD_REQUEST);
    }
}
