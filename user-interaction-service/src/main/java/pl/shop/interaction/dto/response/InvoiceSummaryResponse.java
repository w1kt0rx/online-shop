package pl.shop.interaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceSummaryResponse(
        Long id,
        String invoiceNumber,
        String orderNumber,
        String status,
        BigDecimal totalGross,
        String currency,
        LocalDateTime issuedAt
) {
}
