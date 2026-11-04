package pl.shop.invoice.kafka.event;

public record CustomerData(
        String firstName,
        String lastName,
        String email
) {
}
