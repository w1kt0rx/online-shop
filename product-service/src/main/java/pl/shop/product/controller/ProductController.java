package pl.shop.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.request.PaginationRequest;
import pl.shop.product.dto.request.UpdateProductRequest;
import pl.shop.product.dto.response.PageResponse;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.dto.response.ProductSummaryResponse;
import pl.shop.product.service.ProductService;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("Received POST request to create product with name = {}", request.name());
        return productService.createProduct(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        log.info("Received GET request to fetch product with id = {}", id);
        return productService.getProduct(id);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResponse<ProductSummaryResponse> getAllProducts(@RequestParam(required = false) String category,
                                                               @Valid @ModelAttribute PaginationRequest paginationRequest) {
        log.info("Received GET request to fetch products, category = {}", category);
        return productService.getAllProducts(category, paginationRequest);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        log.info("Received PUT request to update product with id = {}", id);
        return productService.updateProduct(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        log.info("Received DELETE request to delete product with id = {}", id);
        productService.deleteProduct(id);
    }
}
