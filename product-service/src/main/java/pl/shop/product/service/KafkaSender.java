package pl.shop.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.shop.product.model.Product;

@Component
@Slf4j
public class KafkaSender {

    @Value("${product-service.created-products.topic:created-products}")
    private String createdProductsTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductCreated(Product product) {
        log.info("Send notification to topic: {}, with data: {}", createdProductsTopic, product);
        kafkaTemplate.send(createdProductsTopic, product.getName(), product);
    }

}