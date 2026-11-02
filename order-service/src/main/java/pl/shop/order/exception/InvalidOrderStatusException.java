package pl.shop.order.exception;

import org.springframework.http.HttpStatus;
import pl.shop.order.model.OrderStatus;

public class InvalidOrderStatusException extends OrderServiceException {
    public InvalidOrderStatusException(Long orderId, OrderStatus status) {
        super("INVALID_ORDER_STATUS", "Order with id = " + orderId + " cannot be modified in status " + status, HttpStatus.CONFLICT);
    }
}
