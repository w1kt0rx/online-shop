package pl.shop.product.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuoteRequest(
        List<@NotNull(message = "Configuration ID cannot be null") Long> configurationIds
) {
    public QuoteRequest {
        configurationIds = configurationIds == null ? List.of() : configurationIds;
    }
}
