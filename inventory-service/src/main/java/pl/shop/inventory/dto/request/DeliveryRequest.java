package pl.shop.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DeliveryRequest(
        @NotNull(message = "Delivery quantity is required")
        @Min(value = 1, message = "Delivery quantity must be at least 1")
        Integer quantity
) {
}
