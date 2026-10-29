package pl.shop.product.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ProductServiceException {
    public ProductNotFoundException(Long id) {
        super("PRODUCT_NOT_FOUND", "Couldn't find product with id: " + id, HttpStatus.NOT_FOUND);
    }
}
