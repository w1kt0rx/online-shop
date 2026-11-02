package pl.shop.order.kafka.event;

public record AddressData(
        String street,
        String city,
        String postalCode,
        String country
) {
}
