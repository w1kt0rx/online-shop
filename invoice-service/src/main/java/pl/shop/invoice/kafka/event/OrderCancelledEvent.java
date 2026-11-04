package pl.shop.invoice.kafka.event;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
        Long orderId,
        String orderNumber,
        LocalDateTime cancelledAt
) {
}
