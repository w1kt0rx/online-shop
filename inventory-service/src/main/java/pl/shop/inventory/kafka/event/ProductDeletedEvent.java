package pl.shop.inventory.kafka.event;

public record ProductDeletedEvent(
        Long productId,
        String name
) {
}
