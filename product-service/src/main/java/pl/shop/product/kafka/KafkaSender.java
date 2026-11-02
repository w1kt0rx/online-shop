package pl.shop.product.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.shop.product.kafka.event.ProductCreatedEvent;
import pl.shop.product.kafka.event.ProductDeletedEvent;
import pl.shop.product.model.Product;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSender {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.product-created}")
    private String productCreatedTopic;

    @Value("${app.kafka.topics.product-deleted}")
    private String productDeletedTopic;

    public void sendProductCreated(Product product) {
        ProductCreatedEvent event = new ProductCreatedEvent(product.getId(), product.getName());
        log.info("Sending ProductCreated event to topic = {}, productId = {}", productCreatedTopic, event.productId());
        kafkaTemplate.send(productCreatedTopic, String.valueOf(event.productId()), event);
    }

    public void sendProductDeleted(Product product) {
        ProductDeletedEvent event = new ProductDeletedEvent(product.getId(), product.getName());
        log.info("Sending ProductDeleted event to topic = {}, productId = {}", productDeletedTopic, event.productId());
        kafkaTemplate.send(productDeletedTopic, String.valueOf(event.productId()), event);
    }
}
