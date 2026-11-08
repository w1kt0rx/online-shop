package pl.shop.interaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long orderId,
        String orderNumber,
        String status,
        BuyerResponse buyer,
        AddressResponse billingAddress,
        List<InvoiceLineResponse> lines,
        BigDecimal totalNet,
        BigDecimal totalVat,
        BigDecimal totalGross,
        String currency,
        LocalDateTime issuedAt,
        LocalDateTime cancelledAt
) {
}
