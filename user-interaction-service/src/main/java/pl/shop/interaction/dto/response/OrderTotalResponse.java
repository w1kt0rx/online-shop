package pl.shop.interaction.dto.response;

import java.math.BigDecimal;

public record OrderTotalResponse(
        BigDecimal netPrice,
        BigDecimal vatAmount,
        BigDecimal grossPrice,
        String currency
) {
}
