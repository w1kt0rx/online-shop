package pl.shop.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.request.PaginationRequest;
import pl.shop.product.dto.request.UpdateProductRequest;
import pl.shop.product.dto.response.PageResponse;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.dto.response.ProductSummaryResponse;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.kafka.KafkaSender;
import pl.shop.product.mapper.PriceMapper;
import pl.shop.product.mapper.ProductMapper;
import pl.shop.product.model.Product;
import pl.shop.product.repository.ProductRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final PriceMapper priceMapper;
    private final KafkaSender kafkaSender;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.debug("Attempting to create product with name = {}", request.name());
        Product saved = productRepository.save(productMapper.toEntity(request));
        kafkaSender.sendProductCreated(saved);
        log.info("Product created with id = {}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        log.debug("Attempting to fetch product with id = {}", id);
        Product product = fetchProduct(id);
        log.info("Product with id = {} fetched", id);
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProducts(String category, PaginationRequest paginationRequest) {
        log.debug("Attempting to fetch products, category = {}", category);
        Page<Product> products = category == null || category.isBlank()
                ? productRepository.findAll(paginationRequest.toPageable())
                : productRepository.findByCategoryIgnoreCase(category, paginationRequest.toPageable());
        log.info("Fetched {} products", products.getNumberOfElements());
        return PageResponse.from(products.map(productMapper::toSummaryResponse));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.debug("Attempting to update product with id = {}", id);
        Product product = fetchProduct(id);
        product.update(
                request.name(),
                request.category(),
                request.vendor(),
                priceMapper.toEntity(request.price()),
                request.sellableSeparately()
        );
        log.info("Product with id = {} updated", id);
        return productMapper.toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.debug("Attempting to delete product with id = {}", id);
        Product product = fetchProduct(id);
        productRepository.delete(product);
        productRepository.flush();
        kafkaSender.sendProductDeleted(product);
        log.info("Product with id = {} deleted", id);
    }

    private Product fetchProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
