package pl.shop.cart.exception;

import org.springframework.http.HttpStatus;

public class CartNotFoundException extends CartServiceException {
    public CartNotFoundException(Long cartId) {
        super("CART_NOT_FOUND", "Cart with id = " + cartId + " doesn't exist", HttpStatus.NOT_FOUND);
    }
}
