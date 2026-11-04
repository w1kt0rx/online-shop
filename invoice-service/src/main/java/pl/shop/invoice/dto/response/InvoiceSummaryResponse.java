package pl.shop.invoice.dto.response;

import pl.shop.invoice.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceSummaryResponse(
        Long id,
        String invoiceNumber,
        String orderNumber,
        InvoiceStatus status,
        BigDecimal totalGross,
        String currency,
        LocalDateTime issuedAt
) {
}
