package pl.shop.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePriceRequest(
        @NotNull(message = "Net price cannot be null")
        @DecimalMin(value = "0.00", message = "Net price must be greater than or equal to 0.00")
        @Digits(integer = 10, fraction = 2, message = "Net price must have up to 10 integer digits and 2 decimal places")
        BigDecimal netPrice,

        @NotNull(message = "VAT rate cannot be null")
        @DecimalMin(value = "0.00", message = "VAT rate cannot be negative")
        @DecimalMax(value = "1.00", message = "VAT rate cannot exceed 1.00 (100%)")
        BigDecimal vatRate,

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "[A-Z]{3}", message = "Currency code must consist of exactly 3 uppercase letters (e.g. USD, EUR)")
        String currency
) {
}
