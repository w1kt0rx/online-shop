package pl.shop.cart.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddCartItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        List<@NotNull(message = "Configuration ID cannot be null") Long> configurationIds,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity cannot exceed 99")
        Integer quantity
) {
    public AddCartItemRequest {
        configurationIds = configurationIds == null ? List.of() : configurationIds;
    }
}
