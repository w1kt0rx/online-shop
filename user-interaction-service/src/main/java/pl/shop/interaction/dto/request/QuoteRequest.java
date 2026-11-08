package pl.shop.interaction.dto.request;

import java.util.List;

public record QuoteRequest(
        List<Long> configurationIds
) {
    public QuoteRequest {
        configurationIds = configurationIds == null ? List.of() : configurationIds;
    }
}
