package pl.shop.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Street is required")
        @Size(max = 255, message = "Street cannot exceed 255 characters")
        String street,
        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,
        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "\\d{2}-\\d{3}", message = "Postal code must match the XX-XXX format")
        String postalCode,
        @NotBlank(message = "Country is required")
        @Pattern(regexp = "[A-Z]{2}", message = "Country must be a valid 2-letter ISO code (e.g. PL)")
        String country
) {
}