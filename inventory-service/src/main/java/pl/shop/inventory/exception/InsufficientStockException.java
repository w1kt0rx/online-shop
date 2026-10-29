package pl.shop.inventory.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends InventoryServiceException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super("INSUFFICIENT_STOCK", "Insufficient stock for product id = " +
                productId + ". Requested: " + requested + ", available: " + available, HttpStatus.CONFLICT);
    }
}
