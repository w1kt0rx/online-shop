package pl.shop.cart.exception;

import org.springframework.http.HttpStatus;

public class CartAlreadyOrderedException extends CartServiceException {
    public CartAlreadyOrderedException(Long cartId) {
        super("CART_ALREADY_ORDERED", "Cart with id = " + cartId + " is already ordered", HttpStatus.CONFLICT);
    }
}
