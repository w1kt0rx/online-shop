package pl.shop.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeQuantityRequest(
        @NotNull
        @NotNull @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity
) {
}
