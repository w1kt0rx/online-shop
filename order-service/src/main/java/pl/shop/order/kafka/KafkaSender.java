package pl.shop.order.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.shop.order.kafka.event.OrderCancelledEvent;
import pl.shop.order.kafka.event.OrderPlacedEvent;
import pl.shop.order.mapper.OrderEventMapper;
import pl.shop.order.model.Order;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSender {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderEventMapper orderEventMapper;

    @Value("${app.kafka.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${app.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    public void sendOrderPlaced(Order order) {
        OrderPlacedEvent event = orderEventMapper.toOrderPlacedEvent(order);
        log.info("Sending OrderPlaced event to topic = {}, orderNumber = {}, cartId = {}",
                orderPlacedTopic, event.orderNumber(), event.cartId());
        kafkaTemplate.send(orderPlacedTopic, event.orderNumber(), event);
    }

    public void sendOrderCancelled(Order order) {
        OrderCancelledEvent event = orderEventMapper.toOrderCancelledEvent(order);
        log.info("Sending OrderCancelled event to topic = {}, orderNumber = {}",
                orderCancelledTopic, event.orderNumber());
        kafkaTemplate.send(orderCancelledTopic, event.orderNumber(), event);
    }
}