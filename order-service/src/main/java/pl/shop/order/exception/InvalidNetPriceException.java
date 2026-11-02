package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InvalidNetPriceException extends OrderServiceException {
    public InvalidNetPriceException(BigDecimal netPrice) {
        super("INVALID_NET_PRICE", "NetPrice of product cannot be negative. Given netPrice: " + netPrice, HttpStatus.BAD_REQUEST);
    }
}
