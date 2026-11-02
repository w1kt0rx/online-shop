package pl.shop.order.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,
        @NotBlank(message = "Email address is required")
        @Email(message = "Email address must be a valid email format")
        @Size(max = 255, message = "Email address cannot exceed 255 characters")
        String email
) {
}