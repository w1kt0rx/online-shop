package pl.shop.inventory.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.inventory.dto.request.ChangeQuantityRequest;
import pl.shop.inventory.dto.request.DeliveryRequest;
import pl.shop.inventory.dto.response.StockResponse;
import pl.shop.inventory.exception.InvalidQuantityException;
import pl.shop.inventory.exception.StockItemNotFoundException;
import pl.shop.inventory.mapper.InventoryMapper;
import pl.shop.inventory.model.StockItem;
import pl.shop.inventory.repository.InventoryRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InventoryServiceTest {
    private InventoryService inventoryService;
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setup() {
        this.inventoryRepository = Mockito.mock(InventoryRepository.class);
        InventoryMapper inventoryMapper = Mappers.getMapper(InventoryMapper.class);
        this.inventoryService = new InventoryService(inventoryRepository, inventoryMapper);
    }

    @Test
    void getStock_stockItemExists_returnsStockResponse() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(12);
        when(inventoryRepository.findByProductId(4L)).thenReturn(Optional.of(stockItem));
        //when
        StockResponse response = inventoryService.getStock(4L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, response.productId()),
                () -> Assertions.assertEquals(12, response.quantity()),
                () -> Assertions.assertTrue(response.available())
        );
    }

    @Test
    void getStock_quantityZero_returnsNotAvailable() {
        //given
        StockItem stockItem = new StockItem(4L);
        when(inventoryRepository.findByProductId(4L)).thenReturn(Optional.of(stockItem));
        //when
        StockResponse response = inventoryService.getStock(4L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(0, response.quantity()),
                () -> Assertions.assertFalse(response.available())
        );
    }

    @Test
    void getStock_stockItemNotExists_throwsStockItemNotFoundException() {
        //given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());
        //when //then
        StockItemNotFoundException ex = Assertions.assertThrows(
                StockItemNotFoundException.class,
                () -> inventoryService.getStock(999L)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("STOCK_ITEM_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals("Product with id = 999 doesn't exist in the stock", ex.getMessage())
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStocks_someProductsMissing_missingReturnedWithZeroQuantity() {
        //given
        StockItem gpu = new StockItem(4L);
        gpu.changeQuantity(3);
        StockItem ram = new StockItem(7L);
        ram.changeQuantity(20);
        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        when(inventoryRepository.findAllByProductIdIn(any())).thenReturn(List.of(ram, gpu));
        //when
        List<StockResponse> responses = inventoryService.getStocks(List.of(4L, 7L, 99L));
        //then
        Mockito.verify(inventoryRepository).findAllByProductIdIn(idsCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(4L, 7L, 99L), List.copyOf(idsCaptor.getValue())),
                () -> Assertions.assertEquals(3, responses.size()),
                () -> Assertions.assertEquals(new StockResponse(4L, 3, true), responses.get(0)),
                () -> Assertions.assertEquals(new StockResponse(7L, 20, true), responses.get(1)),
                () -> Assertions.assertEquals(new StockResponse(99L, 0, false), responses.get(2))
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStocks_duplicatedIds_eachProductReturnedOnce() {
        //given
        StockItem gpu = new StockItem(4L);
        gpu.changeQuantity(3);
        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        when(inventoryRepository.findAllByProductIdIn(any())).thenReturn(List.of(gpu));
        //when
        List<StockResponse> responses = inventoryService.getStocks(List.of(4L, 4L, 4L));
        //then
        Mockito.verify(inventoryRepository).findAllByProductIdIn(idsCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(4L), List.copyOf(idsCaptor.getValue())),
                () -> Assertions.assertEquals(1, responses.size()),
                () -> Assertions.assertEquals(new StockResponse(4L, 3, true), responses.getFirst())
        );
    }

    @Test
    void changeQuantity_dataCorrect_quantityChanged() {
        //given
        StockItem stockItem = new StockItem(4L);
        when(inventoryRepository.findByProductId(4L)).thenReturn(Optional.of(stockItem));
        //when
        StockResponse response = inventoryService.changeQuantity(4L, new ChangeQuantityRequest(25));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(25, response.quantity()),
                () -> Assertions.assertTrue(response.available()),
                () -> Assertions.assertEquals(25, stockItem.getQuantity())
        );
    }

    @Test
    void changeQuantity_stockItemNotExists_throwsStockItemNotFoundException() {
        //given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());
        //when //then
        Assertions.assertThrows(
                StockItemNotFoundException.class,
                () -> inventoryService.changeQuantity(999L, new ChangeQuantityRequest(25))
        );
    }

    @Test
    void changeQuantity_quantityNegative_throwsInvalidQuantityException() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(5);
        when(inventoryRepository.findByProductId(4L)).thenReturn(Optional.of(stockItem));
        //when //then
        InvalidQuantityException ex = Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> inventoryService.changeQuantity(4L, new ChangeQuantityRequest(-3))
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_QUANTITY", ex.getErrorCode()),
                () -> Assertions.assertEquals(5, stockItem.getQuantity())
        );
    }

    @Test
    void registerDelivery_dataCorrect_quantityIncreased() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(5);
        when(inventoryRepository.findByProductId(4L)).thenReturn(Optional.of(stockItem));
        //when
        StockResponse response = inventoryService.registerDelivery(4L, new DeliveryRequest(10));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, response.productId()),
                () -> Assertions.assertEquals(15, response.quantity()),
                () -> Assertions.assertEquals(15, stockItem.getQuantity())
        );
    }

    @Test
    void registerDelivery_stockItemNotExists_throwsStockItemNotFoundException() {
        //given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());
        //when //then
        Assertions.assertThrows(
                StockItemNotFoundException.class,
                () -> inventoryService.registerDelivery(999L, new DeliveryRequest(10))
        );
    }

    @Test
    void createStockItem_stockItemNotExists_stockItemSavedWithZeroQuantity() {
        //given
        ArgumentCaptor<StockItem> stockItemCaptor = ArgumentCaptor.forClass(StockItem.class);
        when(inventoryRepository.existsByProductId(4L)).thenReturn(false);
        //when
        inventoryService.createStockItem(4L);
        //then
        Mockito.verify(inventoryRepository).save(stockItemCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, stockItemCaptor.getValue().getProductId()),
                () -> Assertions.assertEquals(0, stockItemCaptor.getValue().getQuantity())
        );
    }

    @Test
    void createStockItem_stockItemAlreadyExists_nothingSaved() {
        //given
        when(inventoryRepository.existsByProductId(4L)).thenReturn(true);
        //when
        inventoryService.createStockItem(4L);
        //then
        Mockito.verify(inventoryRepository, Mockito.never()).save(any());
    }

    @Test
    void deleteStockItem_stockItemExists_stockItemDeleted() {
        //given
        StockItem stockItem = new StockItem(4L);
        ReflectionTestUtils.setField(stockItem, "id", 1L);
        ArgumentCaptor<StockItem> stockItemCaptor = ArgumentCaptor.forClass(StockItem.class);
        when(inventoryRepository.findByProductId(4L)).thenReturn(Optional.of(stockItem));
        //when
        inventoryService.deleteStockItem(4L);
        //then
        Mockito.verify(inventoryRepository).delete(stockItemCaptor.capture());
        Assertions.assertSame(stockItem, stockItemCaptor.getValue());
    }

    @Test
    void deleteStockItem_stockItemNotExists_nothingDeleted() {
        //given
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());
        //when
        inventoryService.deleteStockItem(999L);
        //then
        Mockito.verify(inventoryRepository, Mockito.never()).delete(any());
    }
}
