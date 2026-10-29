package pl.shop.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.service.ProductConfigurationService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/products/{productId}/configurations")
public class ProductConfigurationController {
    private final ProductConfigurationService configurationService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<ProductConfigurationResponse> getProductConfiguration(@PathVariable Long productId) {
        log.info("Received GET request to fetch product configuration of product id = {}", productId);
        return configurationService.getProductConfiguration(productId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProductConfigurationResponse createProductConfiguration(@PathVariable Long productId, @Valid @RequestBody CreateProductConfigurationRequest request) {
        log.info("Received POST request to create product configuration for product id = {}", productId);
        return configurationService.createProductConfiguration(productId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{configurationId}")
    public void deleteProductConfiguration(@PathVariable Long productId, @PathVariable Long configurationId) {
        log.info("Received DELETE request to delete product configuration with id = {}, for product id = {}", configurationId, productId);
        configurationService.deleteProductConfiguration(productId, configurationId);
    }
}
