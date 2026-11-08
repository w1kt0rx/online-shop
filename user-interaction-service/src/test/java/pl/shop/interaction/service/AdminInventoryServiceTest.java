package pl.shop.interaction.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.interaction.client.InventoryServiceClient;
import pl.shop.interaction.dto.request.ChangeQuantityRequest;
import pl.shop.interaction.dto.request.DeliveryRequest;
import pl.shop.interaction.dto.response.StockResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class AdminInventoryServiceTest {
    private AdminInventoryService adminInventoryService;
    private InventoryServiceClient inventoryServiceClient;

    @BeforeEach
    void setup() {
        this.inventoryServiceClient = Mockito.mock(InventoryServiceClient.class);
        this.adminInventoryService = new AdminInventoryService(inventoryServiceClient);
    }

    @Test
    void getStock_productIdGiven_stockReturned() {
        //given
        when(inventoryServiceClient.getStock(5L)).thenReturn(new StockResponse(5L, 10, true));
        //when
        StockResponse stock = adminInventoryService.getStock(5L);
        //then
        Assertions.assertEquals(10, stock.quantity());
    }

    @Test
    void getStocks_productIdsGiven_passedToInventoryService() {
        //given
        when(inventoryServiceClient.getStocks(List.of(1L, 2L))).thenReturn(List.of(new StockResponse(1L, 5, true), new StockResponse(2L, 0, false)));
        //when
        List<StockResponse> stocks = adminInventoryService.getStocks(List.of(1L, 2L));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, stocks.size()),
                () -> Assertions.assertFalse(stocks.get(1).available())
        );
    }

    @Test
    void changeQuantity_requestGiven_passedToInventoryService() {
        //given
        ArgumentCaptor<ChangeQuantityRequest> requestCaptor = ArgumentCaptor.forClass(ChangeQuantityRequest.class);
        when(inventoryServiceClient.changeQuantity(anyLong(), any(ChangeQuantityRequest.class))).thenReturn(new StockResponse(5L, 10, true));
        //when
        adminInventoryService.changeQuantity(5L, new ChangeQuantityRequest(10));
        //then
        Mockito.verify(inventoryServiceClient).changeQuantity(Mockito.eq(5L), requestCaptor.capture());
        Assertions.assertEquals(10, requestCaptor.getValue().quantity());
    }

    @Test
    void registerDelivery_requestGiven_passedToInventoryService() {
        //given
        ArgumentCaptor<DeliveryRequest> requestCaptor = ArgumentCaptor.forClass(DeliveryRequest.class);
        when(inventoryServiceClient.registerDelivery(anyLong(), any(DeliveryRequest.class))).thenReturn(new StockResponse(4L, 6, true));
        //when
        StockResponse stock = adminInventoryService.registerDelivery(4L, new DeliveryRequest(5));
        //then
        Mockito.verify(inventoryServiceClient).registerDelivery(Mockito.eq(4L), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(5, requestCaptor.getValue().quantity()),
                () -> Assertions.assertEquals(6, stock.quantity())
        );
    }
}
