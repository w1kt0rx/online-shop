package pl.shop.product.kafka.event;

public record ProductDeletedEvent(
        Long productId,
        String name
) {
}
