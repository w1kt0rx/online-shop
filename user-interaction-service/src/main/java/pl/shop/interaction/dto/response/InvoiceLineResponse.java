package pl.shop.interaction.dto.response;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        Long id,
        String productName,
        String description,
        int quantity,
        BigDecimal unitNetPrice,
        BigDecimal vatRate,
        BigDecimal netAmount,
        BigDecimal vatAmount,
        BigDecimal grossAmount
) {
}
