package pl.shop.order.dto.response;

import java.math.BigDecimal;

public record CartTotalResponse(
        BigDecimal netPrice,
        BigDecimal vatAmount,
        BigDecimal grossPrice,
        String currency
) {
}
