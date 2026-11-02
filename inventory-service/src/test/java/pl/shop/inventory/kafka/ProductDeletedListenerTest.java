package pl.shop.inventory.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.inventory.kafka.event.ProductDeletedEvent;
import pl.shop.inventory.service.InventoryService;

public class ProductDeletedListenerTest {
    private ProductDeletedListener listener;
    private InventoryService inventoryService;

    @BeforeEach
    void setup() {
        this.inventoryService = Mockito.mock(InventoryService.class);
        this.listener = new ProductDeletedListener(inventoryService);
    }

    @Test
    void onProductDeleted_eventReceived_stockItemDeletedForProduct() {
        //given
        ProductDeletedEvent event = new ProductDeletedEvent(4L, "RTX 4070");
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        //when
        listener.onProductDeleted(event);
        //then
        Mockito.verify(inventoryService).deleteStockItem(productIdCaptor.capture());
        Assertions.assertEquals(4L, productIdCaptor.getValue());
    }
}
