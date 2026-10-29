package pl.shop.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductConfigurationRequest(
        @NotNull(message = "Component ID is required")
        Long componentId,
        @NotBlank(message = "Slot name is required")
        @Size(max = 100, message = "Slot name must not exceed 100 characters")
        String slot,
        boolean defaultComponent,
        boolean multiSelect
) {}