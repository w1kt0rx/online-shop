package pl.shop.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.request.QuoteRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.dto.response.QuoteResponse;
import pl.shop.product.service.ProductConfigurationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products/{productId}/configurations")
@RequiredArgsConstructor
public class ProductConfigurationController {
    private final ProductConfigurationService configurationService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<ProductConfigurationResponse> getProductConfigurations(@PathVariable Long productId) {
        log.info("Received GET request to fetch configurations of product with id = {}", productId);
        return configurationService.getProductConfigurations(productId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProductConfigurationResponse createProductConfiguration(@PathVariable Long productId,
                                                                   @Valid @RequestBody CreateProductConfigurationRequest request) {
        log.info("Received POST request to create configuration for product with id = {}", productId);
        return configurationService.createProductConfiguration(productId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{configurationId}")
    public void deleteProductConfiguration(@PathVariable Long productId, @PathVariable Long configurationId) {
        log.info("Received DELETE request to delete configuration with id = {} of product with id = {}", configurationId, productId);
        configurationService.deleteProductConfiguration(productId, configurationId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/quote")
    public QuoteResponse quote(@PathVariable Long productId, @Valid @RequestBody QuoteRequest request) {
        log.info("Received POST request to quote product with id = {}", productId);
        return configurationService.quote(productId, request);
    }
}
