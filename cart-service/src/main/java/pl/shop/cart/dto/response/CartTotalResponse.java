package pl.shop.cart.dto.response;

import java.math.BigDecimal;

public record CartTotalResponse(
        BigDecimal netPrice,
        BigDecimal vatAmount,
        BigDecimal grossPrice,
        String currency
) {
}
