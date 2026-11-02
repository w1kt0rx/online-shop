package pl.shop.inventory.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.shop.inventory.exception.InvalidQuantityException;

public class StockItemTest {

    @Test
    void create_productIdCorrect_stockItemWithZeroQuantityCreated() {
        //given
        Long productId = 4L;
        //when
        StockItem stockItem = new StockItem(productId);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, stockItem.getProductId()),
                () -> Assertions.assertEquals(0, stockItem.getQuantity()),
                () -> Assertions.assertFalse(stockItem.isInStock())
        );
    }

    @Test
    void create_productIdNull_throwsIllegalArgumentException() {
        //given
        Long productId = null;
        //when //then
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new StockItem(productId)
        );
        Assertions.assertEquals("Product id must be positive: null", ex.getMessage());
    }

    @Test
    void create_productIdNotPositive_throwsIllegalArgumentException() {
        //given
        Long productId = 0L;
        //when //then
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new StockItem(productId)
        );
        Assertions.assertEquals("Product id must be positive: 0", ex.getMessage());
    }

    @Test
    void changeQuantity_quantityCorrect_quantityChanged() {
        //given
        StockItem stockItem = new StockItem(4L);
        //when
        stockItem.changeQuantity(15);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(15, stockItem.getQuantity()),
                () -> Assertions.assertTrue(stockItem.isInStock())
        );
    }

    @Test
    void changeQuantity_quantityZero_quantityChangedAndNotInStock() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(10);
        //when
        stockItem.changeQuantity(0);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(0, stockItem.getQuantity()),
                () -> Assertions.assertFalse(stockItem.isInStock())
        );
    }

    @Test
    void changeQuantity_quantityNegative_throwsInvalidQuantityExceptionAndQuantityUnchanged() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(10);
        //when //then
        InvalidQuantityException ex = Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> stockItem.changeQuantity(-1)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_QUANTITY", ex.getErrorCode()),
                () -> Assertions.assertEquals("Quantity cannot be negative: -1", ex.getMessage()),
                () -> Assertions.assertEquals(10, stockItem.getQuantity())
        );
    }

    @Test
    void increase_amountCorrect_quantityIncreased() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(10);
        //when
        stockItem.increase(5);
        //then
        Assertions.assertEquals(15, stockItem.getQuantity());
    }

    @Test
    void increase_amountZero_throwsInvalidQuantityExceptionAndQuantityUnchanged() {
        //given
        StockItem stockItem = new StockItem(4L);
        stockItem.changeQuantity(10);
        //when //then
        InvalidQuantityException ex = Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> stockItem.increase(0)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Amount must be greater than 0: 0", ex.getMessage()),
                () -> Assertions.assertEquals(10, stockItem.getQuantity())
        );
    }
}
