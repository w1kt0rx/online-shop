package pl.shop.order.kafka.event;

public record CustomerData(
        String firstName,
        String lastName,
        String email
) {
}
