package pl.shop.inventory.exception;

import org.springframework.http.HttpStatus;

public class StockItemNotFoundException extends InventoryServiceException {
    public StockItemNotFoundException(Long productId) {
        super("STOCK_ITEM_NOT_FOUND", "Product with id = " + productId + " doesn't exists in the stock", HttpStatus.BAD_REQUEST);
    }
}
