package pl.shop.interaction.dto.request;

public record PlaceOrderRequest(
        Long cartId,
        CustomerRequest customer,
        AddressRequest shippingAddress
) {
}
