package pl.shop.interaction.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.interaction.client.InventoryServiceClient;
import pl.shop.interaction.client.ProductServiceClient;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.QuoteRequest;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.dto.response.PriceResponse;
import pl.shop.interaction.dto.response.ProductDetailsResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.dto.response.ProductSummaryResponse;
import pl.shop.interaction.dto.response.QuoteResponse;
import pl.shop.interaction.dto.response.StockResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class CatalogServiceTest {
    private CatalogService catalogService;
    private ProductServiceClient productServiceClient;
    private InventoryServiceClient inventoryServiceClient;

    @BeforeEach
    void setup() {
        this.productServiceClient = Mockito.mock(ProductServiceClient.class);
        this.inventoryServiceClient = Mockito.mock(InventoryServiceClient.class);
        this.catalogService = new CatalogService(productServiceClient, inventoryServiceClient);
    }

    @Test
    void getProducts_categoryAndPaginationGiven_passedToProductService() {
        //given
        PageResponse<ProductSummaryResponse> page = new PageResponse<>(List.of(), 1, 5, 0L, 0);
        when(productServiceClient.getProducts(any(), any(), any(), any(), any())).thenReturn(page);
        //when
        PageResponse<ProductSummaryResponse> result = catalogService.getProducts("Electronics", new PaginationRequest(1, 5, "name", "DESC"));
        //then
        Mockito.verify(productServiceClient).getProducts("Electronics", 1, 5, "name", "DESC");
        Assertions.assertSame(page, result);
    }

    @Test
    void getProductDetails_stockExists_productWithStockReturned() {
        //given
        ProductResponse product = new ProductResponse(5L, "Gaming PC Pro", "Computer", "MyShop",
                new PriceResponse(new BigDecimal("4800.00"), new BigDecimal("0.23"), new BigDecimal("1104.00"), new BigDecimal("5904.00"), "PLN"),
                true, List.of());
        when(productServiceClient.getProduct(5L)).thenReturn(product);
        when(inventoryServiceClient.getStock(5L)).thenReturn(new StockResponse(5L, 10, true));
        //when
        ProductDetailsResponse details = catalogService.getProductDetails(5L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertSame(product, details.product()),
                () -> Assertions.assertEquals(10, details.stock().quantity()),
                () -> Assertions.assertTrue(details.stock().available())
        );
    }

    @Test
    void getProductDetails_stockNotFound_productReturnedAsUnavailable() {
        //given
        ProductResponse product = new ProductResponse(5L, "Gaming PC Pro", "Computer", "MyShop", null, true, List.of());
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8082/inventory/5", Map.of(), null, StandardCharsets.UTF_8, null);
        when(productServiceClient.getProduct(5L)).thenReturn(product);
        when(inventoryServiceClient.getStock(5L)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));
        //when
        ProductDetailsResponse details = catalogService.getProductDetails(5L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(5L, details.stock().productId()),
                () -> Assertions.assertEquals(0, details.stock().quantity()),
                () -> Assertions.assertFalse(details.stock().available())
        );
    }

    @Test
    void getProductDetails_productNotFound_exceptionPropagatedAndInventoryNotCalled() {
        //given
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8081/products/999", Map.of(), null, StandardCharsets.UTF_8, null);
        when(productServiceClient.getProduct(999L)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));
        //when //then
        Assertions.assertThrows(
                FeignException.NotFound.class,
                () -> catalogService.getProductDetails(999L)
        );
        Mockito.verifyNoInteractions(inventoryServiceClient);
    }

    @Test
    void getConfigurations_productGiven_passedToProductService() {
        //given
        when(productServiceClient.getConfigurations(5L)).thenReturn(List.of());
        //when
        catalogService.getConfigurations(5L);
        //then
        Mockito.verify(productServiceClient).getConfigurations(5L);
    }

    @Test
    void quote_requestGiven_passedToProductService() {
        //given
        QuoteRequest request = new QuoteRequest(List.of(2L, 4L));
        QuoteResponse quote = new QuoteResponse(5L, "Gaming PC Pro", List.of(), null, List.of(5L, 2L, 4L));
        ArgumentCaptor<QuoteRequest> requestCaptor = ArgumentCaptor.forClass(QuoteRequest.class);
        when(productServiceClient.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        //when
        QuoteResponse result = catalogService.quote(5L, request);
        //then
        Mockito.verify(productServiceClient).quote(Mockito.eq(5L), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(2L, 4L), requestCaptor.getValue().configurationIds()),
                () -> Assertions.assertSame(quote, result)
        );
    }
}
