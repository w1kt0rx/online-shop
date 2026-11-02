package pl.shop.order.dto.response;

import pl.shop.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        int itemCount,
        BigDecimal grossPrice,
        String currency,
        LocalDateTime createdAt
) {
}
