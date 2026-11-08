package pl.shop.interaction.dto.request;

import java.math.BigDecimal;

public record PriceRequest(
        BigDecimal netPrice,
        BigDecimal vatRate,
        String currency
) {
}
