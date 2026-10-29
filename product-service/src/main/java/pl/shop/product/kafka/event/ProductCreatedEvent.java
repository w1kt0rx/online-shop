package pl.shop.product.kafka.event;

public record ProductCreatedEvent(
        Long productId,
        String name
) {
}
