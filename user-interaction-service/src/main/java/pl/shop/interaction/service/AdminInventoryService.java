package pl.shop.interaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.shop.interaction.client.InventoryServiceClient;
import pl.shop.interaction.dto.request.ChangeQuantityRequest;
import pl.shop.interaction.dto.request.DeliveryRequest;
import pl.shop.interaction.dto.response.StockResponse;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInventoryService {
    private final InventoryServiceClient inventoryServiceClient;

    public StockResponse getStock(Long productId) {
        log.debug("Fetching stock of product with id = {}", productId);
        return inventoryServiceClient.getStock(productId);
    }

    public List<StockResponse> getStocks(List<Long> productIds) {
        log.debug("Fetching stock of products with ids = {}", productIds);
        return inventoryServiceClient.getStocks(productIds);
    }

    public StockResponse changeQuantity(Long productId, ChangeQuantityRequest request) {
        log.debug("Changing quantity of product with id = {}", productId);
        return inventoryServiceClient.changeQuantity(productId, request);
    }

    public StockResponse registerDelivery(Long productId, DeliveryRequest request) {
        log.debug("Registering delivery of product with id = {}", productId);
        return inventoryServiceClient.registerDelivery(productId, request);
    }
}
