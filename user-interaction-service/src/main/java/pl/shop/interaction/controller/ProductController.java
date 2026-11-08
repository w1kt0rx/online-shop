package pl.shop.interaction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.QuoteRequest;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.dto.response.ProductConfigurationResponse;
import pl.shop.interaction.dto.response.ProductDetailsResponse;
import pl.shop.interaction.dto.response.ProductSummaryResponse;
import pl.shop.interaction.dto.response.QuoteResponse;
import pl.shop.interaction.service.CatalogService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final CatalogService catalogService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResponse<ProductSummaryResponse> getProducts(@RequestParam(required = false) String category,
                                                            @ModelAttribute PaginationRequest pagination) {
        log.info("Received GET request to browse products, category = {}", category);
        return catalogService.getProducts(category, pagination);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{productId}")
    public ProductDetailsResponse getProduct(@PathVariable Long productId) {
        log.info("Received GET request to fetch product with id = {}", productId);
        return catalogService.getProductDetails(productId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{productId}/configurations")
    public List<ProductConfigurationResponse> getConfigurations(@PathVariable Long productId) {
        log.info("Received GET request to fetch configurations of product with id = {}", productId);
        return catalogService.getConfigurations(productId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{productId}/quote")
    public QuoteResponse quote(@PathVariable Long productId, @RequestBody QuoteRequest request) {
        log.info("Received POST request to quote product with id = {}", productId);
        return catalogService.quote(productId, request);
    }
}
