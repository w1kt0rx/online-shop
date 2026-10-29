package pl.shop.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.shop.inventory.kafka.event.ProductDeletedEvent;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductDeletedListener {
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${app.kafka.topics.product-deleted}")
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Received ProductDeleted event, productId = {}", event.productId());
        inventoryService.deleteStockProduct(event.productId());
    }
}
