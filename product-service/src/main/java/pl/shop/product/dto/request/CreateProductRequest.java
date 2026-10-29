package pl.shop.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String category,

        @NotBlank(message = "Vendor is required")
        @Size(max = 100, message = "Vendor name must not exceed 100 characters")
        String vendor,

        @NotNull(message = "Price details are required")
        @Valid
        CreatePriceRequest price,

        @NotNull(message = "Sellable separately flag must be specified")
        Boolean sellableSeparately
) {
}
