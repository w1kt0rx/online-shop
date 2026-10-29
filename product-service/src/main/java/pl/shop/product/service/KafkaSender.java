package pl.shop.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.shop.product.kafka.event.ProductCreatedEvent;
import pl.shop.product.model.Product;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaSender {

    @Value("${app.kafka.topics.product-created}")
    private String productCreatedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductCreated(Product product) {
        ProductCreatedEvent event = new ProductCreatedEvent(product.getId(), product.getName());
        log.info("Sending ProductCreated to topic: {}, productId = {}", productCreatedTopic, event.productId());
        kafkaTemplate.send(productCreatedTopic, String.valueOf(event.productId()), event);
    }

}