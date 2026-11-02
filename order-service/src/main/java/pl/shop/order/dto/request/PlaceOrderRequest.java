package pl.shop.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull(message = "Cart ID is required")
        Long cartId,
        @NotNull(message = "Customer details are required")
        @Valid
        CustomerRequest customer,
        @NotNull(message = "Shipping address is required")
        @Valid
        AddressRequest shippingAddress
) {
}
