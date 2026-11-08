package pl.shop.interaction.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.shop.interaction.client.InventoryServiceClient;
import pl.shop.interaction.client.ProductServiceClient;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.QuoteRequest;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.dto.response.ProductConfigurationResponse;
import pl.shop.interaction.dto.response.ProductDetailsResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.dto.response.ProductSummaryResponse;
import pl.shop.interaction.dto.response.QuoteResponse;
import pl.shop.interaction.dto.response.StockResponse;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

    public PageResponse<ProductSummaryResponse> getProducts(String category, PaginationRequest pagination) {
        log.debug("Fetching products, category = {}", category);
        return productServiceClient.getProducts(category, pagination.page(), pagination.size(),
                pagination.sortBy(), pagination.direction());
    }

    public ProductDetailsResponse getProductDetails(Long productId) {
        log.debug("Fetching details of product with id = {}", productId);
        ProductResponse product = productServiceClient.getProduct(productId);
        StockResponse stock = fetchStock(productId);
        log.info("Details of product with id = {} fetched, available = {}", productId, stock.available());
        return new ProductDetailsResponse(product, stock);
    }

    public List<ProductConfigurationResponse> getConfigurations(Long productId) {
        log.debug("Fetching configurations of product with id = {}", productId);
        return productServiceClient.getConfigurations(productId);
    }

    public QuoteResponse quote(Long productId, QuoteRequest request) {
        log.debug("Quoting product with id = {}, configurations = {}", productId, request.configurationIds());
        return productServiceClient.quote(productId, request);
    }

    private StockResponse fetchStock(Long productId) {
        try {
            return inventoryServiceClient.getStock(productId);
        } catch (FeignException.NotFound ex) {
            log.warn("No stock item for product with id = {}, treating as unavailable", productId);
            return new StockResponse(productId, 0, false);
        }
    }
}
