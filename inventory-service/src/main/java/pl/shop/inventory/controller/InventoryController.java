package pl.shop.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
    public List<StockResponse> getStocks(@RequestParam("productIds") List<Long> productIds) {
        log.info("Received GET request to fetch stock of products with ids = {}", productIds);
        return inventoryService.getStocks(productIds);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{productId}")
    public StockResponse changeQuantity(@PathVariable Long productId, @Valid @RequestBody ChangeQuantityRequest request) {
        log.info("Received PUT request to change quantity of product with id = {}", productId);
        return inventoryService.changeQuantity(productId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{productId}/deliveries")
    public StockResponse registerDelivery(@PathVariable Long productId, @Valid @RequestBody DeliveryRequest request) {
        log.info("Received POST request to register delivery of product with id = {}", productId);
        return inventoryService.registerDelivery(productId, request);
    }
}
