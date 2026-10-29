package pl.shop.product.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InvalidVatRateException extends ProductServiceException {
    public InvalidVatRateException(BigDecimal vatRate) {
        super("INVALID_VAT_RATE", "Vat rate has to be in range 0 - 1. Given rate equal to: " + vatRate, HttpStatus.BAD_REQUEST);
    }
}
