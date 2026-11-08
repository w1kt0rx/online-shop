package pl.shop.interaction.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.interaction.client.ProductServiceClient;
import pl.shop.interaction.dto.request.CreateProductConfigurationRequest;
import pl.shop.interaction.dto.request.CreateProductRequest;
import pl.shop.interaction.dto.request.PriceRequest;
import pl.shop.interaction.dto.request.UpdateProductRequest;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

public class AdminCatalogServiceTest {
    private AdminCatalogService adminCatalogService;
    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setup() {
        this.productServiceClient = Mockito.mock(ProductServiceClient.class);
        this.adminCatalogService = new AdminCatalogService(productServiceClient);
    }

    @Test
    void createProduct_requestGiven_passedToProductService() {
        //given
        CreateProductRequest request = new CreateProductRequest("RTX 4070", "Electronics", "NVIDIA",
                new PriceRequest(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ArgumentCaptor<CreateProductRequest> requestCaptor = ArgumentCaptor.forClass(CreateProductRequest.class);
        //when
        adminCatalogService.createProduct(request);
        //then
        Mockito.verify(productServiceClient).createProduct(requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("RTX 4070", requestCaptor.getValue().name()),
                () -> Assertions.assertEquals(new BigDecimal("2275.00"), requestCaptor.getValue().price().netPrice())
        );
    }

    @Test
    void updateProduct_requestGiven_passedToProductService() {
        //given
        UpdateProductRequest request = new UpdateProductRequest("RTX 4070 Super", "Electronics", "NVIDIA",
                new PriceRequest(new BigDecimal("2600.00"), new BigDecimal("0.23"), "PLN"), true);
        ArgumentCaptor<UpdateProductRequest> requestCaptor = ArgumentCaptor.forClass(UpdateProductRequest.class);
        //when
        adminCatalogService.updateProduct(2L, request);
        //then
        Mockito.verify(productServiceClient).updateProduct(Mockito.eq(2L), requestCaptor.capture());
        Assertions.assertEquals("RTX 4070 Super", requestCaptor.getValue().name());
    }

    @Test
    void deleteProduct_productIdGiven_passedToProductService() {
        //given
        Long productId = 6L;
        //when
        adminCatalogService.deleteProduct(productId);
        //then
        Mockito.verify(productServiceClient).deleteProduct(6L);
    }

    @Test
    void createConfiguration_requestGiven_passedToProductService() {
        //given
        CreateProductConfigurationRequest request = new CreateProductConfigurationRequest(1L, "Graphics card", true, false);
        ArgumentCaptor<CreateProductConfigurationRequest> requestCaptor = ArgumentCaptor.forClass(CreateProductConfigurationRequest.class);
        //when
        adminCatalogService.createConfiguration(5L, request);
        //then
        Mockito.verify(productServiceClient).createConfiguration(Mockito.eq(5L), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, requestCaptor.getValue().componentId()),
                () -> Assertions.assertEquals("Graphics card", requestCaptor.getValue().slot()),
                () -> Assertions.assertTrue(requestCaptor.getValue().defaultComponent())
        );
    }

    @Test
    void deleteConfiguration_idsGiven_passedToProductService() {
        //given
        Long productId = 5L;
        //when
        adminCatalogService.deleteConfiguration(productId, 3L);
        //then
        Mockito.verify(productServiceClient).deleteConfiguration(5L, 3L);
        Mockito.verify(productServiceClient, Mockito.never()).deleteProduct(anyLong());
        Mockito.verify(productServiceClient, Mockito.never()).createConfiguration(anyLong(), any());
    }
}
