package pl.shop.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.inventory.dto.request.ChangeQuantityRequest;
import pl.shop.inventory.dto.request.DeliveryRequest;
import pl.shop.inventory.dto.response.StockResponse;
import pl.shop.inventory.exception.InvalidQuantityException;
import pl.shop.inventory.exception.StockItemNotFoundException;
import pl.shop.inventory.service.InventoryService;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InventoryControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void getStock_stockItemExists_returnsStock() throws Exception {
        //given
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        when(inventoryService.getStock(4L)).thenReturn(new StockResponse(4L, 12, true));
        //when //then
        mockMvc.perform(get("/inventory/{productId}", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.available").value(true));
        Mockito.verify(inventoryService).getStock(productIdCaptor.capture());
        Assertions.assertEquals(4L, productIdCaptor.getValue());
    }

    @Test
    void getStock_stockItemNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        when(inventoryService.getStock(999L)).thenThrow(new StockItemNotFoundException(999L));
        //when //then
        mockMvc.perform(get("/inventory/{productId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product with id = 999 doesn't exist in the stock"));
    }

    @Test
    void getStock_productIdNotNumber_returnsBadRequest() throws Exception {
        //given
        String productId = "abc";
        //when //then
        mockMvc.perform(get("/inventory/{productId}", productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message", Matchers.containsString("productId")));
        Mockito.verify(inventoryService, Mockito.never()).getStock(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStocks_productIdsGiven_returnsStockList() throws Exception {
        //given
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        when(inventoryService.getStocks(any())).thenReturn(List.of(
                new StockResponse(4L, 3, true),
                new StockResponse(99L, 0, false)
        ));
        //when //then
        mockMvc.perform(get("/inventory").param("productIds", "4", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productId").value(4L))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].productId").value(99L))
                .andExpect(jsonPath("$[1].quantity").value(0))
                .andExpect(jsonPath("$[1].available").value(false));
        Mockito.verify(inventoryService).getStocks(idsCaptor.capture());
        Assertions.assertEquals(List.of(4L, 99L), idsCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStocks_commaSeparatedIds_idsParsed() throws Exception {
        //given
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        when(inventoryService.getStocks(any())).thenReturn(List.of());
        //when //then
        mockMvc.perform(get("/inventory").param("productIds", "1,4,6"))
                .andExpect(status().isOk());
        Mockito.verify(inventoryService).getStocks(idsCaptor.capture());
        Assertions.assertEquals(List.of(1L, 4L, 6L), idsCaptor.getValue());
    }

    @Test
    void getStocks_productIdsMissing_returnsBadRequest() throws Exception {
        //given
        String url = "/inventory";
        //when //then
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Missing request parameter: productIds"));
        Mockito.verify(inventoryService, Mockito.never()).getStocks(any());
    }

    @Test
    void changeQuantity_dataCorrect_returnsChangedStock() throws Exception {
        //given
        ChangeQuantityRequest request = new ChangeQuantityRequest(25);
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<ChangeQuantityRequest> requestCaptor = ArgumentCaptor.forClass(ChangeQuantityRequest.class);
        when(inventoryService.changeQuantity(anyLong(), any(ChangeQuantityRequest.class)))
                .thenReturn(new StockResponse(4L, 25, true));
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(25));
        Mockito.verify(inventoryService).changeQuantity(productIdCaptor.capture(), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, productIdCaptor.getValue()),
                () -> Assertions.assertEquals(25, requestCaptor.getValue().quantity())
        );
    }

    @Test
    void changeQuantity_quantityNegative_returnsBadRequestWithMessage() throws Exception {
        //given
        ChangeQuantityRequest request = new ChangeQuantityRequest(-1);
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Quantity cannot be negative")));
        Mockito.verify(inventoryService, Mockito.never()).changeQuantity(anyLong(), any());
    }

    @Test
    void changeQuantity_quantityNull_returnsBadRequestWithMessage() throws Exception {
        //given
        ChangeQuantityRequest request = new ChangeQuantityRequest(null);
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Quantity is required")));
        Mockito.verify(inventoryService, Mockito.never()).changeQuantity(anyLong(), any());
    }

    @Test
    void changeQuantity_bodyMalformed_returnsBadRequest() throws Exception {
        //given
        String body = "{\"quantity\":";
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void changeQuantity_stockItemNotExists_returnsNotFound() throws Exception {
        //given
        ChangeQuantityRequest request = new ChangeQuantityRequest(25);
        when(inventoryService.changeQuantity(anyLong(), any(ChangeQuantityRequest.class)))
                .thenThrow(new StockItemNotFoundException(999L));
        //when //then
        mockMvc.perform(put("/inventory/{productId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"));
    }

    @Test
    void registerDelivery_dataCorrect_returnsIncreasedStock() throws Exception {
        //given
        DeliveryRequest request = new DeliveryRequest(10);
        ArgumentCaptor<Long> productIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<DeliveryRequest> requestCaptor = ArgumentCaptor.forClass(DeliveryRequest.class);
        when(inventoryService.registerDelivery(anyLong(), any(DeliveryRequest.class)))
                .thenReturn(new StockResponse(4L, 15, true));
        //when //then
        mockMvc.perform(post("/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4L))
                .andExpect(jsonPath("$.quantity").value(15));
        Mockito.verify(inventoryService).registerDelivery(productIdCaptor.capture(), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, productIdCaptor.getValue()),
                () -> Assertions.assertEquals(10, requestCaptor.getValue().quantity())
        );
    }

    @Test
    void registerDelivery_quantityZero_returnsBadRequestWithMessage() throws Exception {
        //given
        DeliveryRequest request = new DeliveryRequest(0);
        //when //then
        mockMvc.perform(post("/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Delivery quantity must be at least 1")));
        Mockito.verify(inventoryService, Mockito.never()).registerDelivery(anyLong(), any());
    }

    @Test
    void registerDelivery_invalidQuantity_returnsBadRequestWithMessage() throws Exception {
        //given
        DeliveryRequest request = new DeliveryRequest(10);
        when(inventoryService.registerDelivery(anyLong(), any(DeliveryRequest.class)))
                .thenThrow(new InvalidQuantityException("Amount must be greater than 0: 0"));
        //when //then
        mockMvc.perform(post("/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_QUANTITY"));
    }

    @Test
    void registerDelivery_stockItemNotExists_returnsNotFound() throws Exception {
        //given
        DeliveryRequest request = new DeliveryRequest(10);
        when(inventoryService.registerDelivery(anyLong(), any(DeliveryRequest.class)))
                .thenThrow(new StockItemNotFoundException(999L));
        //when //then
        mockMvc.perform(post("/inventory/{productId}/deliveries", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STOCK_ITEM_NOT_FOUND"));
    }
}
