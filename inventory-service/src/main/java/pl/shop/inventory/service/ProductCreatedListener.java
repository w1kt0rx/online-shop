package pl.shop.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.shop.inventory.kafka.event.ProductCreatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCreatedListener {
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${app.kafka.topics.product-created}")
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Received ProductCreated event, productId = {}", event.productId());
        inventoryService.createStockProduct(event.productId());
    }
}