package pl.shop.inventory.kafka.event;

public record ProductCreatedEvent(
        Long productId,
        String name
) {
}
