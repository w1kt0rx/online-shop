package pl.shop.product.dto.response;

import java.math.BigDecimal;

public record PriceResponse(
        BigDecimal netPrice,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal grossPrice,
        String currency
) {
}
