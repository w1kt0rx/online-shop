package pl.shop.order.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends OrderServiceException {
    public OrderNotFoundException(Long orderId) {
        super("ORDER_NOT_FOUND", "Order with id = " + orderId + " doesn't exist", HttpStatus.NOT_FOUND);
    }
}
