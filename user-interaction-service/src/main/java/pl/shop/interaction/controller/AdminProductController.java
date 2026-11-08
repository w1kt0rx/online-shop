package pl.shop.interaction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.interaction.dto.request.CreateProductConfigurationRequest;
import pl.shop.interaction.dto.request.CreateProductRequest;
import pl.shop.interaction.dto.request.UpdateProductRequest;
import pl.shop.interaction.dto.response.ProductConfigurationResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.service.AdminCatalogService;

@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final AdminCatalogService adminCatalogService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProductResponse createProduct(@RequestBody CreateProductRequest request) {
        log.info("Received POST request to create product with name = {}", request.name());
        return adminCatalogService.createProduct(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable Long productId, @RequestBody UpdateProductRequest request) {
        log.info("Received PUT request to update product with id = {}", productId);
        return adminCatalogService.updateProduct(productId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{productId}")
    public void deleteProduct(@PathVariable Long productId) {
        log.info("Received DELETE request to delete product with id = {}", productId);
        adminCatalogService.deleteProduct(productId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{productId}/configurations")
    public ProductConfigurationResponse createConfiguration(@PathVariable Long productId,
                                                            @RequestBody CreateProductConfigurationRequest request) {
        log.info("Received POST request to create configuration for product with id = {}", productId);
        return adminCatalogService.createConfiguration(productId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{productId}/configurations/{configurationId}")
    public void deleteConfiguration(@PathVariable Long productId, @PathVariable Long configurationId) {
        log.info("Received DELETE request to delete configuration with id = {} of product with id = {}", configurationId, productId);
        adminCatalogService.deleteConfiguration(productId, configurationId);
    }
}
