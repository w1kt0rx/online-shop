package pl.shop.interaction.dto.request;

import java.util.List;

public record AddCartItemRequest(
        Long productId,
        List<Long> configurationIds,
        Integer quantity
) {
    public AddCartItemRequest {
        configurationIds = configurationIds == null ? List.of() : configurationIds;
    }
}
