package pl.shop.product.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.product.dto.request.CreatePriceRequest;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.request.PageRequestDto;
import pl.shop.product.dto.request.UpdateProductRequest;
import pl.shop.product.dto.response.PageDto;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.dto.response.ProductSummaryResponse;
import pl.shop.product.exception.InvalidNetPriceException;
import pl.shop.product.exception.ProductNotFoundException;
import pl.shop.product.mapper.PriceMapper;
import pl.shop.product.mapper.ProductConfigurationMapper;
import pl.shop.product.mapper.ProductMapper;
import pl.shop.product.model.Price;
import pl.shop.product.model.Product;
import pl.shop.product.model.ProductConfiguration;
import pl.shop.product.model.SortDirection;
import pl.shop.product.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ProductServiceTest {
    private ProductService productService;
    private ProductRepository productRepository;
    private ProductMapper productMapper;

    @BeforeEach
    void setup() {
        this.productRepository = Mockito.mock(ProductRepository.class);
        this.productMapper = Mappers.getMapper(ProductMapper.class);
        ReflectionTestUtils.setField(productMapper, "priceMapper", Mappers.getMapper(PriceMapper.class));
        ReflectionTestUtils.setField(productMapper, "productConfigurationMapper", Mappers.getMapper(ProductConfigurationMapper.class));
        this.productService = new ProductService(productRepository, productMapper);
    }

    @Test
    void createProduct_dataCorrect_productSavedAndResponseReturned() {
        // given
        CreateProductRequest request = new CreateProductRequest("Gaming PC Pro", "Computer", "MyShop",
                new CreatePriceRequest(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 1L);
            return product;
        });
        // when
        ProductResponse response = productService.createProduct(request);
        // then
        Mockito.verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals("Gaming PC Pro", response.name()),
                () -> Assertions.assertEquals("Computer", response.category()),
                () -> Assertions.assertEquals("MyShop", response.vendor()),
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), response.price().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("1104.00"), response.price().vatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("5904.00"), response.price().grossPrice()),
                () -> Assertions.assertTrue(response.sellableSeparately()),
                () -> Assertions.assertTrue(response.configurations().isEmpty()),
                () -> Assertions.assertEquals("Gaming PC Pro", saved.getName()),
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), saved.getPrice().getNetPrice()),
                () -> Assertions.assertEquals("PLN", saved.getPrice().getCurrency()),
                () -> Assertions.assertTrue(saved.getConfigurations().isEmpty())
        );
    }

    @Test
    void getProduct_productExists_returnsProductResponse() {
        // given
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product gpu = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(gpu, "id", 4L);
        ProductConfiguration gpuConfiguration = pc.addConfiguration(gpu, "Garphics card", true, false);
        ReflectionTestUtils.setField(gpuConfiguration, "id", 10L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(pc));
        // when
        ProductResponse response = productService.getProduct(1L);
        // then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals("Gaming PC Pro", response.name()),
                () -> Assertions.assertEquals(1, response.configurations().size()),
                () -> Assertions.assertEquals(10L, response.configurations().getFirst().id()),
                () -> Assertions.assertEquals("Garphics card", response.configurations().getFirst().slot()),
                () -> Assertions.assertEquals(4L, response.configurations().getFirst().componentId()),
                () -> Assertions.assertEquals("RTX 4070", response.configurations().getFirst().componentName()),
                () -> Assertions.assertTrue(response.configurations().getFirst().defaultComponent())
        );
    }

    @Test
    void getProduct_productNotExists_throwsProductNotFoundException() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProduct(999L)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("PRODUCT_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage())
        );
    }

    @Test
    void getAllProducts_categoryNull_findAllCalled() {
        // given
        PageRequestDto pageRequest = new PageRequestDto(null, null, null, null);
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        Product phone = new Product("Phone Z", "Smartfon", "Samsung",
                new Price(new BigDecimal("2500.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(phone, "id", 3L);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pc, phone), PageRequest.of(0, 20), 2));
        // when
        PageDto<ProductSummaryResponse> result = productService.getAllProducts(null, pageRequest);
        // then
        Mockito.verify(productRepository, Mockito.never()).findByCategoryIgnoreCase(any(), any());
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, result.content().size()),
                () -> Assertions.assertEquals("Gaming PC Pro", result.content().getFirst().name()),
                () -> Assertions.assertEquals("Phone Z", result.content().get(1).name()),
                () -> Assertions.assertEquals(0, result.pageNumber()),
                () -> Assertions.assertEquals(20, result.pageSize()),
                () -> Assertions.assertEquals(2L, result.totalElements()),
                () -> Assertions.assertEquals(1, result.totalPages())
        );
    }

    @Test
    void getAllProducts_categoryBlank_findAllCalled() {
        // given
        PageRequestDto pageRequest = new PageRequestDto(null, null, null, null);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        // when
        productService.getAllProducts("   ", pageRequest);
        // then
        Mockito.verify(productRepository).findAll(any(Pageable.class));
        Mockito.verify(productRepository, Mockito.never()).findByCategoryIgnoreCase(any(), any());
    }

    @Test
    void getAllProducts_categoryGiven_findByCategoryIgnoreCaseCalled() {
        // given
        PageRequestDto pageRequest = new PageRequestDto(null, null, null, null);
        Product pc = new Product("Gaming PC Pro", "Computer", "MyShop",
                new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(pc, "id", 1L);
        ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
        when(productRepository.findByCategoryIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pc), PageRequest.of(0, 20), 1));
        // when
        PageDto<ProductSummaryResponse> result = productService.getAllProducts("Computer", pageRequest);
        // then
        Mockito.verify(productRepository).findByCategoryIgnoreCase(categoryCaptor.capture(), any(Pageable.class));
        Mockito.verify(productRepository, Mockito.never()).findAll(any(Pageable.class));
        Assertions.assertAll(
                () -> Assertions.assertEquals("Computer", categoryCaptor.getValue()),
                () -> Assertions.assertEquals(1, result.content().size()),
                () -> Assertions.assertEquals("Computer", result.content().getFirst().category())
        );
    }

    @Test
    void getAllProducts_pageRequestGiven_pageableWithSortPassedToRepository() {
        // given
        PageRequestDto pageRequest = new PageRequestDto(1, 5, "name", SortDirection.DESC);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));
        // when
        productService.getAllProducts(null, pageRequest);
        // then
        Mockito.verify(productRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, pageable.getPageNumber()),
                () -> Assertions.assertEquals(5, pageable.getPageSize()),
                () -> Assertions.assertNotNull(pageable.getSort().getOrderFor("name")),
                () -> Assertions.assertEquals(Sort.Direction.DESC, Objects.requireNonNull(pageable.getSort().getOrderFor("name")).getDirection())
        );
    }

    @Test
    void getAllProducts_noProducts_returnsEmptyPage() {
        // given
        PageRequestDto pageRequest = new PageRequestDto(null, null, null, null);
        when(productRepository.findByCategoryIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        // when
        PageDto<ProductSummaryResponse> result = productService.getAllProducts("TV", pageRequest);
        // then
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.content().isEmpty()),
                () -> Assertions.assertEquals(0L, result.totalElements()),
                () -> Assertions.assertEquals(0, result.totalPages())
        );
    }

    @Test
    void updateProduct_dataCorrect_productFieldsUpdated() {
        // given
        Product product = new Product("Office PC", "Computer", "MyShop",
                new Price(new BigDecimal("2400.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(product, "id", 2L);
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "Dell",
                new CreatePriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), false);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        // when
        ProductResponse response = productService.updateProduct(2L, request);
        // then
        Assertions.assertAll(
                () -> Assertions.assertEquals(2L, response.id()),
                () -> Assertions.assertEquals("Office PC v2", response.name()),
                () -> Assertions.assertEquals("Dell", response.vendor()),
                () -> Assertions.assertEquals(new BigDecimal("2600.00"), response.price().netPrice()),
                () -> Assertions.assertEquals(new BigDecimal("3198.00"), response.price().grossPrice()),
                () -> Assertions.assertFalse(response.sellableSeparately()),
                () -> Assertions.assertEquals("Office PC v2", product.getName()),
                () -> Assertions.assertEquals("Dell", product.getVendor()),
                () -> Assertions.assertEquals(new BigDecimal("2600.00"), product.getPrice().getNetPrice()),
                () -> Assertions.assertFalse(product.isSellableSeparately())
        );
    }

    @Test
    void updateProduct_productNotExists_throwsProductNotFoundException() {
        // given
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "Dell",
                new CreatePriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(999L, request)
        );
        Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage());
    }

    @Test
    void updateProduct_netPriceNegative_throwsInvalidNetPriceExceptionAndProductUnchanged() {
        // given
        Product product = new Product("Office PC", "Computer", "MyShop",
                new Price(new BigDecimal("2400.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(product, "id", 2L);
        UpdateProductRequest request = new UpdateProductRequest("Office PC v2", "Computer", "Dell",
                new CreatePriceRequest(new BigDecimal("-1"), new BigDecimal("0.23"), "PLN"), true);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        // when + then
        InvalidNetPriceException ex = Assertions.assertThrows(
                InvalidNetPriceException.class,
                () -> productService.updateProduct(2L, request)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_NET_PRICE", ex.getErrorCode()),
                () -> Assertions.assertEquals("Office PC", product.getName()),
                () -> Assertions.assertEquals(new BigDecimal("2400.00"), product.getPrice().getNetPrice())
        );
    }

    @Test
    void deleteProduct_productExists_productDeletedAndFlushed() {
        // given
        Product product = new Product("Headphones ABC", "Electronics", "Sony",
                new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(product, "id", 5L);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        // when
        productService.deleteProduct(5L);
        // then
        InOrder inOrder = Mockito.inOrder(productRepository);
        inOrder.verify(productRepository).delete(productCaptor.capture());
        inOrder.verify(productRepository).flush();
        Assertions.assertSame(product, productCaptor.getValue());
    }

    @Test
    void deleteProduct_productNotExists_throwsProductNotFoundException() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        // when + then
        ProductNotFoundException ex = Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(999L)
        );
        Assertions.assertEquals("Couldn't find product with id: 999", ex.getMessage());
        Mockito.verify(productRepository, Mockito.never()).delete(any());
        Mockito.verify(productRepository, Mockito.never()).flush();
    }
}
