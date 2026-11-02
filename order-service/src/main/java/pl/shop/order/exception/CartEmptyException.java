package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

public class CartEmptyException extends OrderServiceException {
    public CartEmptyException(Long cartId) {
        super("CART_EMPTY", "Cart with id = " + cartId + " is empty", HttpStatus.CONFLICT);
    }
}
