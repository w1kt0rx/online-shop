package pl.shop.inventory.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.inventory.kafka.event.ProductCreatedEvent;
import pl.shop.inventory.service.InventoryService;

public class ProductCreatedListenerTest {
    private ProductCreatedListener listener;
    private InventoryService inventoryService;

    @BeforeEach
    void setup() {
        this.inventoryService = Mockito.mock(InventoryService.class);
        this.listener = new ProductCreatedListener(inventoryService);
    }

    @Test
    void onProductCreated_eventReceived_stockItemCreatedForProduct() {
        //given
        ProductCreatedEvent event = new ProductCreatedEvent(4L, "RTX 4070");
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        //when
        listener.onProductCreated(event);
        //then
        Mockito.verify(inventoryService).createStockItem(productIdCaptor.capture());
        Assertions.assertEquals(4L, productIdCaptor.getValue());
    }
}
