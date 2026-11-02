package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

public class CartNotActiveException extends OrderServiceException {
    public CartNotActiveException(Long cartId) {
        super("CART_NOT_ACTIVE", "Cart with id = " + cartId + " is not active", HttpStatus.CONFLICT);
    }
}
