package pl.shop.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockReservationRequest(
        @NotBlank(message = "Order number is required")
        String orderNumber,
        @NotEmpty(message = "Reservation must contain at least one item")
        List<@Valid StockReservationItemRequest> items
) {
}
