package pl.shop.interaction.dto.request;

public record AddressRequest(
        String street,
        String city,
        String postalCode,
        String country
) {
}
