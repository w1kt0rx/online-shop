package pl.shop.interaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.shop.interaction.client.ProductServiceClient;
import pl.shop.interaction.dto.request.CreateProductConfigurationRequest;
import pl.shop.interaction.dto.request.CreateProductRequest;
import pl.shop.interaction.dto.request.UpdateProductRequest;
import pl.shop.interaction.dto.response.ProductConfigurationResponse;
import pl.shop.interaction.dto.response.ProductResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCatalogService {
    private final ProductServiceClient productServiceClient;

    public ProductResponse createProduct(CreateProductRequest request) {
        log.debug("Creating product with name = {}", request.name());
        return productServiceClient.createProduct(request);
    }

    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        log.debug("Updating product with id = {}", productId);
        return productServiceClient.updateProduct(productId, request);
    }

    public void deleteProduct(Long productId) {
        log.debug("Deleting product with id = {}", productId);
        productServiceClient.deleteProduct(productId);
    }

    public ProductConfigurationResponse createConfiguration(Long productId, CreateProductConfigurationRequest request) {
        log.debug("Creating configuration for product with id = {}", productId);
        return productServiceClient.createConfiguration(productId, request);
    }

    public void deleteConfiguration(Long productId, Long configurationId) {
        log.debug("Deleting configuration with id = {} of product with id = {}", configurationId, productId);
        productServiceClient.deleteConfiguration(productId, configurationId);
    }
}
