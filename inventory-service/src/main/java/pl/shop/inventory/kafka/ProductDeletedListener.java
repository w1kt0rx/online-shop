package pl.shop.inventory.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.shop.inventory.kafka.event.ProductDeletedEvent;
import pl.shop.inventory.service.InventoryService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDeletedListener {
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${app.kafka.topics.product-deleted}")
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Received ProductDeleted event, productId = {}", event.productId());
        inventoryService.deleteStockItem(event.productId());
    }
}
