package pl.shop.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.request.PageRequestDto;
import pl.shop.product.dto.request.UpdateProductRequest;
import pl.shop.product.dto.response.PageDto;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.dto.response.ProductSummaryResponse;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.mapper.ProductMapper;
import pl.shop.product.model.Price;
import pl.shop.product.model.Product;
import pl.shop.product.repository.ProductRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final KafkaSender kafkaSender;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.debug("Attempting to create product name='{}'", request.name());
        Product savedProduct = productRepository.save(productMapper.toEntity(request));
        kafkaSender.sendProductCreated(savedProduct);
        log.info("Product created successfully with id={}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        log.debug("Attempting to fetch product with id = {}", id);
        Product product = fetchProduct(id);
        log.info("Product with id = {} fetch successfully", product.getId());
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageDto<ProductSummaryResponse> getAllProducts(String category, PageRequestDto pageRequest) {
        log.debug("Attempting to fetch all products");
        Page<Product> products = (category == null || category.isBlank()) ?
                productRepository.findAll(pageRequest.toPageable()) : productRepository.findByCategoryIgnoreCase(category, pageRequest.toPageable());
        log.info("Products fetch successfully");
        return PageDto.from(products.map(
                productMapper::toSummaryResponse
        ));
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.debug("Attempting to delete product with id = {}", id);
        Product product = fetchProduct(id);
        productRepository.delete(product);
        productRepository.flush();
        log.info("Product with id = {} successfully deleted", product.getId());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest updateProductRequest) {
        log.debug("Attempting to update product with id = {}", id);
        Product product = fetchProduct(id);
        product.update(
                updateProductRequest.name(),
                updateProductRequest.category(),
                updateProductRequest.vendor(),
                new Price(updateProductRequest.price().netPrice(),
                        updateProductRequest.price().vatRate(),
                        updateProductRequest.price().currency()),
                updateProductRequest.sellableSeparately()
        );
        log.info("Product with id = {} successfully updated", product.getId());
        return productMapper.toResponse(product);
    }

    private Product fetchProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(
                        () -> new ProductNotFoundException(id)
                );
    }

}
