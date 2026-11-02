package pl.shop.cart.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.shop.cart.kafka.event.OrderPlacedEvent;
import pl.shop.cart.service.CartService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedListener {
    private final CartService cartService;

    @KafkaListener(topics = "${app.kafka.topics.order-placed}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlaced event, orderNumber = {}, cartId = {}", event.orderNumber(), event.cartId());
        cartService.markAsOrdered(event.cartId());
    }
}
