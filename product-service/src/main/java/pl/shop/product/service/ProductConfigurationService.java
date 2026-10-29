package pl.shop.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.mapper.ProductConfigurationMapper;
import pl.shop.product.model.Product;
import pl.shop.product.model.ProductConfiguration;
import pl.shop.product.repository.ProductRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductConfigurationService {
    private final ProductRepository productRepository;
    private final ProductConfigurationMapper configurationMapper;

    @Transactional(readOnly = true)
    public List<ProductConfigurationResponse> getProductConfiguration(Long productId) {
        log.debug("Attempting to fetch product configurations for product id = {}", productId);
        Product product = fetchProduct(productId);
        List<ProductConfiguration> configurationList = product.getConfigurations();
        log.info("Product configuration list successfully fetched");
        return configurationList.stream()
                .map(configurationMapper::toDto)
                .toList();
    }

    @Transactional
    public ProductConfigurationResponse createProductConfiguration(Long productId, CreateProductConfigurationRequest configurationRequest) {
        log.debug("Attempting to create product configuration for product id = {}", productId);
        Product product = fetchProduct(productId);
        ProductConfiguration configuration  = product.addConfiguration(
                fetchProduct(configurationRequest.componentId()),
                configurationRequest.slot(),
                configurationRequest.defaultComponent(),
                configurationRequest.multiSelect()
        );
        productRepository.flush();
        log.info("Product configuration successfully created id = {}", configuration.getId());
        return configurationMapper.toDto(configuration);
    }
    @Transactional
    public void deleteProductConfiguration(Long productId, Long configurationId) {
        log.debug("Attempting to delete product configuration with id = {},  for product id = {}",configurationId, productId);
        Product product = fetchProduct(productId);
        product.removeConfiguration(configurationId);
        productRepository.flush();
        log.info("Product configuration with id = {} successfully deleted from product id = {}", configurationId, productId);
    }

    private Product fetchProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(
                        () -> new ProductNotFoundException(productId)
                );
    }
}
