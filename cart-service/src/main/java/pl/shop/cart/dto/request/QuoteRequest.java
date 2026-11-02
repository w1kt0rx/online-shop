package pl.shop.cart.dto.request;

import java.util.List;

public record QuoteRequest(
        List<Long> configurationIds
) {
}
