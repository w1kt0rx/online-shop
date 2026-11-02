package pl.shop.cart.kafka.event;

public record OrderPlacedEvent(
        Long orderId,
        String orderNumber,
        Long cartId
) {
}
