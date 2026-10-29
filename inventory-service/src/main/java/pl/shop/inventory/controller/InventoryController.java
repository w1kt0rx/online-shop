package pl.shop.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.shop.inventory.dto.request.ChangeQuantityRequest;
import pl.shop.inventory.dto.request.DeliveryRequest;
import pl.shop.inventory.dto.response.StockResponse;
import pl.shop.inventory.service.InventoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{productId}")
    public StockResponse getStock(@PathVariable Long productId) {
        log.info("Received GET request to fetch stock of product with id = {}", productId);
        return inventoryService.getStock(productId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<StockResponse> getProductsStock(@RequestParam(value="productIds") List<Long> productIds) {
        log.info("Received GET request to fetch stock of products with ids = {}", productIds);
        return inventoryService.getProductsStock(productIds);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{productId}")
    public StockResponse changeQuantity(@PathVariable Long productId, @Valid @RequestBody ChangeQuantityRequest request) {
        log.info("Received PUT request to update quantity of product with id = {}", productId);
        return inventoryService.updateProductStock(productId, request);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{productId}/deliveries")
    public StockResponse delivery(@PathVariable Long productId, @Valid @RequestBody DeliveryRequest request) {
        log.info("Received POST request to add quantity do the product with id = {}", productId);
        return inventoryService.takeDelivery(productId, request);
    }

}
