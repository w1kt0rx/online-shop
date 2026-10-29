package pl.shop.product.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InvalidNetPriceException extends ProductServiceException {
    public InvalidNetPriceException(BigDecimal netPrice) {
        super("INVALID_NET_PRICE", "NetPrice of product cannot be negative. Given netPrice: " + netPrice, HttpStatus.BAD_REQUEST);
    }
}
