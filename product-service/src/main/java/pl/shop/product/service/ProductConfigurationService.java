package pl.shop.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.request.QuoteRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.dto.response.QuoteResponse;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.mapper.ProductConfigurationMapper;
import pl.shop.product.model.Product;
import pl.shop.product.model.ProductConfiguration;
import pl.shop.product.model.Quote;
import pl.shop.product.repository.ProductRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductConfigurationService {
    private final ProductRepository productRepository;
    private final ProductConfigurationMapper configurationMapper;

    @Transactional(readOnly = true)
    public List<ProductConfigurationResponse> getProductConfigurations(Long productId) {
        log.debug("Attempting to fetch configurations of product with id = {}", productId);
        Product product = fetchProduct(productId);
        log.info("Fetched {} configurations of product with id = {}", product.getConfigurations().size(), productId);
        return product.getConfigurations().stream()
                .map(configurationMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductConfigurationResponse createProductConfiguration(Long productId, CreateProductConfigurationRequest request) {
        log.debug("Attempting to create configuration for product with id = {}", productId);
        Product product = fetchProduct(productId);
        ProductConfiguration configuration = product.addConfiguration(
                fetchProduct(request.componentId()),
                request.slot(),
                request.defaultComponent(),
                request.multiSelect()
        );
        productRepository.flush();
        log.info("Configuration with id = {} created for product with id = {}", configuration.getId(), productId);
        return configurationMapper.toResponse(configuration);
    }

    @Transactional
    public void deleteProductConfiguration(Long productId, Long configurationId) {
        log.debug("Attempting to delete configuration with id = {} of product with id = {}", configurationId, productId);
        Product product = fetchProduct(productId);
        product.removeConfiguration(configurationId);
        productRepository.flush();
        log.info("Configuration with id = {} deleted from product with id = {}", configurationId, productId);
    }

    @Transactional(readOnly = true)
    public QuoteResponse quote(Long productId, QuoteRequest request) {
        log.debug("Attempting to quote product with id = {} and configurations = {}", productId, request.configurationIds());
        Product product = fetchProduct(productId);
        Quote quote = product.quote(request.configurationIds());
        log.info("Quoted product with id = {}, net price = {}", productId, quote.unitPrice().getNetPrice());
        return configurationMapper.toQuoteResponse(product, quote);
    }

    private Product fetchProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
