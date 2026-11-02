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
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public StockResponse getStock(Long productId) {
        log.debug("Attempting to fetch stock of product with id = {}", productId);
        StockItem stockItem = fetchByProductId(productId);
        log.info("Stock of product with id = {} fetched", productId);
        return inventoryMapper.toResponse(stockItem);
    }

    @Transactional(readOnly = true)
    public List<StockResponse> getStocks(List<Long> productIds) {
        log.debug("Attempting to fetch stock of products with ids = {}", productIds);
        List<Long> distinctIds = productIds.stream().distinct().toList();
        Map<Long, StockItem> stockByProductId = findStockByProductId(distinctIds);
        List<StockResponse> responses = distinctIds.stream()
                .map(productId -> toResponseOrEmpty(productId, stockByProductId.get(productId)))
                .toList();
        log.info("Stock fetched for {} products", responses.size());
        return responses;
    }

    @Transactional
    public StockResponse changeQuantity(Long productId, ChangeQuantityRequest request) {
        log.debug("Attempting to change quantity of product with id = {}", productId);
        StockItem stockItem = fetchByProductId(productId);
        stockItem.changeQuantity(request.quantity());
        log.info("Quantity of product with id = {} changed to {}", productId, request.quantity());
        return inventoryMapper.toResponse(stockItem);
    }

    @Transactional
    public StockResponse registerDelivery(Long productId, DeliveryRequest request) {
        log.debug("Attempting to register delivery of product with id = {}", productId);
        StockItem stockItem = fetchByProductId(productId);
        stockItem.increase(request.quantity());
        log.info("Quantity of product with id = {} increased by {}", productId, request.quantity());
        return inventoryMapper.toResponse(stockItem);
    }

    @Transactional
    public void createStockItem(Long productId) {
        if (inventoryRepository.existsByProductId(productId)) {
            log.warn("Stock item for product with id = {} already exists, skipping", productId);
            return;
        }
        inventoryRepository.save(new StockItem(productId));
        log.info("Stock item created for product with id = {}", productId);
    }

    @Transactional
    public void deleteStockItem(Long productId) {
        inventoryRepository.findByProductId(productId)
                .ifPresentOrElse(
                        this::deleteExisting,
                        () -> log.warn("Stock item for product with id = {} doesn't exist, skipping", productId)
                );
    }

    private void deleteExisting(StockItem stockItem) {
        inventoryRepository.delete(stockItem);
        log.info("Stock item deleted for product with id = {}", stockItem.getProductId());
    }

    private Map<Long, StockItem> findStockByProductId(List<Long> productIds) {
        return inventoryRepository.findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(StockItem::getProductId, Function.identity()));
    }

    private StockResponse toResponseOrEmpty(Long productId, StockItem stockItem) {
        return stockItem == null
                ? new StockResponse(productId, 0, false)
                : inventoryMapper.toResponse(stockItem);
    }

    private StockItem fetchByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new StockItemNotFoundException(productId));
    }
}
