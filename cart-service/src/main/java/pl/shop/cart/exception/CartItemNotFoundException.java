package pl.shop.cart.exception;

import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends CartServiceException {
    public CartItemNotFoundException(Long cartId, Long itemId) {
        super("CART_ITEM_NOT_FOUND", "Item with id = " + itemId + " not found in cart with id = " + cartId, HttpStatus.NOT_FOUND);
    }
}
