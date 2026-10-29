package pl.shop.product.exception;

import org.springframework.http.HttpStatus;

public class InvalidConfigurationException extends ProductServiceException {
    public InvalidConfigurationException(String message) {
        super("INVALID_CONFIGURATION", message, HttpStatus.BAD_REQUEST);
    }
}
