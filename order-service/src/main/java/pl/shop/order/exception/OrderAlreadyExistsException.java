package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

public class OrderAlreadyExistsException extends OrderServiceException {
    public OrderAlreadyExistsException(Long cartId) {
        super("ORDER_ALREADY_EXISTS", "Order for cart with id = " + cartId + " already exists", HttpStatus.CONFLICT);
    }
}
