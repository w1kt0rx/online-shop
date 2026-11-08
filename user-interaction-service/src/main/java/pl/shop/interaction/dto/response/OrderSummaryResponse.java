package pl.shop.interaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        String status,
        int itemCount,
        BigDecimal grossPrice,
        String currency,
        LocalDateTime createdAt
) {
}
