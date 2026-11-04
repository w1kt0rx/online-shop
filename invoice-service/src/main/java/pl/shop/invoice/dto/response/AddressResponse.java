package pl.shop.invoice.dto.response;

public record AddressResponse(
        String street,
        String city,
        String postalCode,
        String country
) {
}
