package pl.shop.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.inventory.dto.request.ChangeQuantityRequest;
import pl.shop.inventory.dto.request.DeliveryRequest;
import pl.shop.inventory.dto.response.StockResponse;
import pl.shop.inventory.exception.StockItemNotFoundException;
import pl.shop.inventory.mapper.InventoryMapper;
import pl.shop.inventory.model.StockItem;
import pl.shop.inventory.repository.InventoryRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public StockResponse getStock(Long productId) {
        log.debug("Attempting to fetch stock information of product with id = {}", productId);
        StockItem stockItem = fetchByProductId(productId);
        log.info("Successfully fetched stock item with productId = {}", productId);
        return inventoryMapper.toResponse(stockItem);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> getProductsStock(List<Long> productIds) {
        log.debug("Attempting to fetch stock information about products with ids = {}", productIds);
        List<Long> distinctIds = productIds.stream().distinct().toList();

        Map<Long, StockItem> stockByProductId = inventoryRepository.findAllByProductIdIn(distinctIds)
                .stream()
                .collect(Collectors.toMap(StockItem::getProductId, Function.identity()));

        List<StockResponse> responses = distinctIds.stream()
                .map(productId -> {
                    StockItem stockItem = stockByProductId.get(productId);
                    return stockItem == null
                            ? new StockResponse(productId, 0, false)
                            : inventoryMapper.toResponse(stockItem);
                })
                .toList();
        log.info("Stock fetched for {} products", responses.size());
        return responses;
    }

    @Transactional
    public StockResponse updateProductStock(Long productId, ChangeQuantityRequest request) {
        log.debug("Attempting to change quantity of product with id = {}", productId);
        StockItem stockItem = fetchByProductId(productId);
        stockItem.changeQuantity(request.quantity());
        log.info("Quantity of product with id = {}, successfully changed to {}", productId, request.quantity());
        return inventoryMapper.toResponse(stockItem);
    }

    @Transactional
    public StockResponse takeDelivery(Long productId, DeliveryRequest request) {
        log.debug("Attempting to increase quantity of product with id = {}", productId);
        StockItem stockItem = fetchByProductId(productId);
        stockItem.increase(request.quantity());
        log.info("Quantity of product with id = {}, successfully increased by {}", productId, request.quantity());
        return inventoryMapper.toResponse(stockItem);
    }

    @Transactional
    public void createStockProduct(Long productId) {
        if(inventoryRepository.existsByProductId(productId)) {
            log.warn("Stock item for product id = {} already exists, skipping", productId);
            return;
        }
        inventoryRepository.save(new StockItem(productId));
        log.info("Stock item created for product id = {}", productId);
    }

    private StockItem fetchByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(
                        () -> new StockItemNotFoundException(productId)
                );
    }
}
