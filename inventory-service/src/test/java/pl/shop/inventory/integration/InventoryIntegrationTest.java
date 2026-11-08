package pl.shop.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.inventory.dto.request.ChangeQuantityRequest;
import pl.shop.inventory.dto.request.DeliveryRequest;
import pl.shop.inventory.dto.request.StockReservationItemRequest;
import pl.shop.inventory.dto.request.StockReservationRequest;
import pl.shop.inventory.model.StockItem;
import pl.shop.inventory.repository.InventoryRepository;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InventoryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InventoryRepository inventoryRepository;

    @AfterEach
    void cleanup() {
        inventoryRepository.deleteAll();
    }

    @Test
    void getStock_stockItemExists_returnsStockFromDatabase() throws Exception {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(12);
        inventoryRepository.save(stockItem);
        //when //then
        mockMvc.perform(get("/inventory/{productId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getStock_stockItemNotExists_returnsNotFound() throws Exception {
        //given
        inventoryRepository.save(new StockItem(4L));
        //when //then
        mockMvc.perform(get("/inventory/{productId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product with id = 999 doesn't exist in the stock"));
    }

    @Test
    void getStocks_someProductsMissing_returnsStockAndZeroForMissing() throws Exception {
        //given
        StockItem gpu = new StockItem(4L);
        gpu.changeQuantity(3);
        inventoryRepository.save(gpu);
        inventoryRepository.save(new StockItem(6L));
        //when //then
        mockMvc.perform(get("/inventory").param("productIds", "4", "6", "99", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].productId").value(4L))
                .andExpect(jsonPath("$[0].quantity").value(3))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].productId").value(6L))
                .andExpect(jsonPath("$[1].quantity").value(0))
                .andExpect(jsonPath("$[1].available").value(false))
                .andExpect(jsonPath("$[2].productId").value(99L))
                .andExpect(jsonPath("$[2].quantity").value(0))
                .andExpect(jsonPath("$[2].available").value(false));
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, inventoryRepository.count()),
                () -> Assertions.assertTrue(inventoryRepository.findByProductId(99L).isEmpty())
        );
    }

    @Test
    void getStocks_productIdsParamMissing_returnsBadRequest() throws Exception {
        //when //then
        mockMvc.perform(get("/inventory"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Missing request parameter: productIds"));
    }

    @Test
    void changeQuantity_dataCorrect_quantityPersisted() throws Exception {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(12);
        inventoryRepository.save(stockItem);
        ChangeQuantityRequest request = new ChangeQuantityRequest(5);
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.available").value(true));
        Assertions.assertEquals(5, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity());
    }

    @Test
    void changeQuantity_quantityNegative_badRequestAndQuantityUnchanged() throws Exception {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(12);
        inventoryRepository.save(stockItem);
        ChangeQuantityRequest request = new ChangeQuantityRequest(-1);
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Quantity cannot be negative")));
        Assertions.assertEquals(12, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity());
    }

    @Test
    void changeQuantity_stockItemNotExists_returnsNotFoundAndNothingCreated() throws Exception {
        //given
        ChangeQuantityRequest request = new ChangeQuantityRequest(5);
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product with id = 999 doesn't exist in the stock"));
        Assertions.assertEquals(0, inventoryRepository.count());
    }

    @Test
    void registerDelivery_dataCorrect_quantityIncreased() throws Exception {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(3);
        inventoryRepository.save(stockItem);
        DeliveryRequest request = new DeliveryRequest(7);
        //when //then
        mockMvc.perform(post("/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.available").value(true));
        Assertions.assertEquals(10, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity());
    }

    @Test
    void registerDelivery_quantityZero_badRequestAndQuantityUnchanged() throws Exception {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(3);
        inventoryRepository.save(stockItem);
        DeliveryRequest request = new DeliveryRequest(0);
        //when //then
        mockMvc.perform(post("/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Delivery quantity must be at least 1")));
        Assertions.assertEquals(3, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity());
    }

    @Test
    void reserve_enoughStock_allQuantitiesDecreased() throws Exception {
        //given
        StockItem pc = new StockItem(1L);
        pc.changeQuantity(5);
        inventoryRepository.save(pc);
        StockItem gpu = new StockItem(4L);
        gpu.changeQuantity(2);
        inventoryRepository.save(gpu);
        StockReservationRequest request = new StockReservationRequest("ORD-1", List.of(
                new StockReservationItemRequest(1L, 3),
                new StockReservationItemRequest(4L, 2)
        ));
        //when //then
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].productId").value(4L))
                .andExpect(jsonPath("$[1].quantity").value(0))
                .andExpect(jsonPath("$[1].available").value(false));
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, inventoryRepository.findByProductId(1L).orElseThrow().getQuantity()),
                () -> Assertions.assertEquals(0, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity())
        );
    }

    @Test
    void reserve_oneItemInsufficient_conflictAndNoQuantityChanged() throws Exception {
        //given
        StockItem pc = new StockItem(1L);
        pc.changeQuantity(5);
        inventoryRepository.save(pc);
        StockItem gpu = new StockItem(4L);
        gpu.changeQuantity(4);
        inventoryRepository.save(gpu);
        StockReservationRequest request = new StockReservationRequest("ORD-2", List.of(
                new StockReservationItemRequest(1L, 3),
                new StockReservationItemRequest(4L, 10)
        ));
        //when //then
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 4. Requested: 10, available: 4"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(5, inventoryRepository.findByProductId(1L).orElseThrow().getQuantity()),
                () -> Assertions.assertEquals(4, inventoryRepository.findByProductId(4L).orElseThrow().getQuantity())
        );
    }

    @Test
    void reserve_productNotInStock_conflictAndNoQuantityChanged() throws Exception {
        //given
        StockItem pc = new StockItem(1L);
        pc.changeQuantity(5);
        inventoryRepository.save(pc);
        StockReservationRequest request = new StockReservationRequest("ORD-3", List.of(
                new StockReservationItemRequest(1L, 1),
                new StockReservationItemRequest(99L, 1)
        ));
        //when //then
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 99. Requested: 1, available: 0"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(5, inventoryRepository.findByProductId(1L).orElseThrow().getQuantity()),
                () -> Assertions.assertEquals(1, inventoryRepository.count())
        );
    }

    @Test
    void reserve_itemsEmpty_badRequestAndNoQuantityChanged() throws Exception {
        //given
        StockItem pc = new StockItem(1L);
        pc.changeQuantity(5);
        inventoryRepository.save(pc);
        StockReservationRequest request = new StockReservationRequest("ORD-4", List.of());
        //when //then
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Reservation must contain at least one item")));
        Assertions.assertEquals(5, inventoryRepository.findByProductId(1L).orElseThrow().getQuantity());
    }

    @Test
    void release_someItemsMissing_existingIncreasedAndMissingSkipped() throws Exception {
        //given
        StockItem pc = new StockItem(1L);
        pc.changeQuantity(5);
        inventoryRepository.save(pc);
        StockReservationRequest request = new StockReservationRequest("ORD-5", List.of(
                new StockReservationItemRequest(1L, 2),
                new StockReservationItemRequest(99L, 3)
        ));
        //when //then
        mockMvc.perform(post("/inventory/releases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(1L))
                .andExpect(jsonPath("$[0].quantity").value(7))
                .andExpect(jsonPath("$[0].available").value(true));
        Assertions.assertAll(
                () -> Assertions.assertEquals(7, inventoryRepository.findByProductId(1L).orElseThrow().getQuantity()),
                () -> Assertions.assertTrue(inventoryRepository.findByProductId(99L).isEmpty()),
                () -> Assertions.assertEquals(1, inventoryRepository.count())
        );
    }
}
