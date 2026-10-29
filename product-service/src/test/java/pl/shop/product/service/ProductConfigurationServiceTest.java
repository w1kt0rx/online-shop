package pl.shop.product.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.product.dto.request.CreateProductConfigurationRequest;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.exception.ConfigurationNotFoundException;
import pl.shop.product.exception.InvalidConfigurationException;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.mapper.ProductConfigurationMapper;
import pl.shop.product.model.Price;
import pl.shop.product.model.Product;
import pl.shop.product.model.ProductConfiguration;
import pl.shop.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

public class ProductConfigurationServiceTest {
    private ProductConfigurationService configurationService;
    private ProductRepository productRepository;
    private ProductConfigurationMapper configurationMapper;

    @BeforeEach
    void setup() {
        this.productRepository = Mockito.mock(ProductRepository.class);
        this.configurationMapper = Mappers.getMapper(ProductConfigurationMapper.class);
        this.configurationService = new ProductConfigurationService(productRepository, configurationMapper);
    }

    @Test
    void getProductConfiguration_productExists_returnsMappedList() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4060", "Electronics", "NVIDIA",
                new Price(new BigDecimal("1220.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 6L);
        Product ram = new Product("RAM 16 GB DDR5", "Electronics", "Kingston",
                new Price(new BigDecimal("203.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(ram, "id", 7L);
        ProductConfiguration gpuConfiguration = pc.addConfiguration(gpu, "Graphics card", true, false);
        ReflectionTestUtils.setField(gpuConfiguration, "id", 1L);
        ProductConfiguration ramConfiguration = pc.addConfiguration(ram, "RAM", true, false);
        ReflectionTestUtils.setField(ramConfiguration, "id", 3L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        // when
        List<ProductConfigurationResponse> responses = configurationService.getProductConfiguration(1L);
        // then
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, responses.size()),
                () -> Assertions.assertEquals(1L, responses.getFirst().id()),
                () -> Assertions.assertEquals("Graphics card", responses.getFirst().slot()),
                () -> Assertions.assertEquals(6L, responses.getFirst().componentId()),
                () -> Assertions.assertEquals("RTX 4060", responses.getFirst().componentName()),
                () -> Assertions.assertEquals(3L, responses.get(1).id()),
                () -> Assertions.assertEquals("RAM", responses.get(1).slot())
        );
    }

    @Test
    void getProductConfiguration_noConfigurations_returnsEmptyList() {
        // given
        Product headphones = new Product("Headphones ABC", "Electronics", "Sony",
                new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(headphones, "id", 5L);
        when(productRepository.findById(5L)).thenReturn(Optional.of(headphones));
        // when
        List<ProductConfigurationResponse> responses = configurationService.getProductConfiguration(5L);
        // then
        Assertions.assertTrue(responses.isEmpty());
    }

    @Test
    void getProductConfiguration_productNotExists_throwsProductNotFoundException() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> configurationService.getProductConfiguration(999L)
        );
        Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage());
    }

    @Test
    void createProductConfiguration_dataCorrect_configurationCreatedWithId() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 4L);
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(4L, "Graphics card", true, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(productRepository.findById(4L)).thenReturn(Optional.of(gpu));
        Mockito.doAnswer(invocation -> {
            ReflectionTestUtils.setField(pc.getConfigurations().getFirst(), "id", 10L);
            return null;
        }).when(productRepository).flush();
        // when
        ProductConfigurationResponse response = configurationService.createProductConfiguration(1L, request);
        // then
        Mockito.verify(productRepository).flush();
        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, response.id()),
                () -> Assertions.assertEquals("Graphics card", response.slot()),
                () -> Assertions.assertEquals(4L, response.componentId()),
                () -> Assertions.assertEquals("RTX 4070", response.componentName()),
                () -> Assertions.assertTrue(response.defaultComponent()),
                () -> Assertions.assertFalse(response.multiSelect()),
                () -> Assertions.assertEquals(1, pc.getConfigurations().size()),
                () -> Assertions.assertSame(gpu, pc.getConfigurations().getFirst().getComponent()),
                () -> Assertions.assertSame(pc, pc.getConfigurations().getFirst().getProduct())
        );
    }

    @Test
    void createProductConfiguration_productNotExists_throwsProductNotFoundException() {
        // given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(4L, "Graphics card", true, false);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> configurationService.createProductConfiguration(999L, request)
        );
        Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage());
        Mockito.verify(productRepository, Mockito.never()).flush();
    }

    @Test
    void createProductConfiguration_componentNotExists_throwsProductNotFoundException() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(999L, "Graphics card", true, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> configurationService.createProductConfiguration(1L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage()),
                () -> Assertions.assertTrue(pc.getConfigurations().isEmpty())
        );
        Mockito.verify(productRepository, Mockito.never()).flush();
    }

    @Test
    void createProductConfiguration_componentSameAsProduct_throwsInvalidConfigurationException() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(1L, "Graphics card", true, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        // when + then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> configurationService.createProductConfiguration(1L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_CONFIGURATION", ex.getErrorCode()),
                () -> Assertions.assertEquals("Product cannot be its own component", ex.getMessage()),
                () -> Assertions.assertTrue(pc.getConfigurations().isEmpty())
        );
        Mockito.verify(productRepository, Mockito.never()).flush();
    }

    @Test
    void createProductConfiguration_componentAlreadyInSlot_throwsInvalidConfigurationException() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 4L);
        ProductConfiguration existing = pc.addConfiguration(gpu, "Graphics card", false, false);
        ReflectionTestUtils.setField(existing, "id", 10L);
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(4L, "Graphics card", false, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        when(productRepository.findById(4L)).thenReturn(Optional.of(gpu));
        // when + then
        InvalidConfigurationException ex = Assertions.assertThrows(
                InvalidConfigurationException.class,
                () -> configurationService.createProductConfiguration(1L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Component already added to slot: Graphics card", ex.getMessage()),
                () -> Assertions.assertEquals(1, pc.getConfigurations().size())
        );
        Mockito.verify(productRepository, Mockito.never()).flush();
    }

    @Test
    void deleteProductConfiguration_configurationExists_configurationRemovedAndFlushed() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 4L);
        ProductConfiguration configuration = pc.addConfiguration(gpu, "Graphics card", false, false);
        ReflectionTestUtils.setField(configuration, "id", 10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        // when
        configurationService.deleteProductConfiguration(1L, 10L);
        // then
        Mockito.verify(productRepository).flush();
        Assertions.assertTrue(pc.getConfigurations().isEmpty());
    }

    @Test
    void deleteProductConfiguration_configurationNotExists_throwsConfigurationNotFoundException() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 4L);
        ProductConfiguration configuration = pc.addConfiguration(gpu, "Graphics card", false, false);
        ReflectionTestUtils.setField(configuration, "id", 10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        // when + then
        ConfigurationNotFoundException ex = Assertions.assertThrows(
                ConfigurationNotFoundException.class,
                () -> configurationService.deleteProductConfiguration(1L, 999L)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("CONFIGURATION_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals("Configuration not found: id = 999", ex.getMessage()),
                () -> Assertions.assertEquals(1, pc.getConfigurations().size())
        );
        Mockito.verify(productRepository, Mockito.never()).flush();
    }

    @Test
    void deleteProductConfiguration_productNotExists_throwsProductNotFoundException() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> configurationService.deleteProductConfiguration(999L, 10L)
        );
        Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage());
        Mockito.verify(productRepository, Mockito.never()).flush();
    }
}