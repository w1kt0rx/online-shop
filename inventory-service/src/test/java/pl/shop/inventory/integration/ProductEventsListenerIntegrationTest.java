package pl.shop.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.inventory.dto.request.DeliveryRequest;
import pl.shop.inventory.kafka.ProductCreatedListener;
import pl.shop.inventory.kafka.ProductDeletedListener;
import pl.shop.inventory.kafka.event.ProductCreatedEvent;
import pl.shop.inventory.kafka.event.ProductDeletedEvent;
import pl.shop.inventory.model.StockItem;
import pl.shop.inventory.repository.InventoryRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductEventsListenerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProductCreatedListener productCreatedListener;
    @Autowired
    private ProductDeletedListener productDeletedListener;

    @AfterEach
    void cleanup() {
        inventoryRepository.deleteAll();
    }

    @Test
    void onProductCreated_newProduct_stockItemCreatedWithZeroQuantity() throws Exception {
        //given
        ProductCreatedEvent event = new ProductCreatedEvent(4L, "RTX 4070");
        //when
        productCreatedListener.onProductCreated(event);
        //then
        StockItem stockItem = inventoryRepository.findByProductId(4L).orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, inventoryRepository.count()),
                () -> Assertions.assertEquals(4L, stockItem.getProductId()),
                () -> Assertions.assertEquals(0, stockItem.getQuantity()),
                () -> Assertions.assertFalse(stockItem.isInStock())
        );
        mockMvc.perform(get("/inventory/{productId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void onProductCreated_stockItemAlreadyExists_existingStockItemKept() throws Exception {
        //given
        StockItem existing = new StockItem(4L);
        existing.changeQuantity(7);
        inventoryRepository.save(existing);
        ProductCreatedEvent event = new ProductCreatedEvent(4L, "RTX 4070");
        //when
        productCreatedListener.onProductCreated(event);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, inventoryRepository.count()),
                () -> Assertions.assertEquals(7, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity())
        );
        mockMvc.perform(get("/inventory/{productId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void onProductDeleted_stockItemExists_stockItemRemoved() throws Exception {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(7);
        inventoryRepository.save(stockItem);
        inventoryRepository.save(new StockItem(6L));
        ProductDeletedEvent event = new ProductDeletedEvent(4L, "RTX 4070");
        //when
        productDeletedListener.onProductDeleted(event);
        //then
        Assertions.assertAll(
                () -> Assertions.assertTrue(inventoryRepository.findByProductId(4L).isEmpty()),
                () -> Assertions.assertTrue(inventoryRepository.findByProductId(6L).isPresent()),
                () -> Assertions.assertEquals(1, inventoryRepository.count())
        );
        mockMvc.perform(get("/inventory/{productId}", 4L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"));
    }

    @Test
    void onProductDeleted_stockItemNotExists_otherStockItemsUntouched() {
        //given
        StockItem stockItem = new StockItem(6L);
        stockItem.changeQuantity(3);
        inventoryRepository.save(stockItem);
        ProductDeletedEvent event = new ProductDeletedEvent(99L, "Unknown");
        //when
        productDeletedListener.onProductDeleted(event);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, inventoryRepository.count()),
                () -> Assertions.assertEquals(3, inventoryRepository.findByProductId(6L).orElseThrow().getQuantity())
        );
    }

    @Test
    void onProductCreatedAndDeleted_fullLifecycle_stockItemCreatedUpdatedAndRemoved() throws Exception {
        //given
        ProductCreatedEvent createdEvent = new ProductCreatedEvent(8L, "RAM 32 GB DDR5");
        ProductDeletedEvent deletedEvent = new ProductDeletedEvent(8L, "RAM 32 GB DDR5");
        DeliveryRequest deliveryRequest = new DeliveryRequest(5);
        //when
        productCreatedListener.onProductCreated(createdEvent);
        mockMvc.perform(post("/inventory/{productId}/deliveries", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
        int quantityAfterDelivery = inventoryRepository.findByProductId(8L).orElseThrow().getQuantity();
        productDeletedListener.onProductDeleted(deletedEvent);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(5, quantityAfterDelivery),
                () -> Assertions.assertTrue(inventoryRepository.findByProductId(8L).isEmpty())
        );
    }
}
