package pl.shop.interaction.controller;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.QuoteRequest;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.dto.response.PriceResponse;
import pl.shop.interaction.dto.response.ProductDetailsResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.dto.response.ProductSummaryResponse;
import pl.shop.interaction.dto.response.QuoteComponentResponse;
import pl.shop.interaction.dto.response.QuoteResponse;
import pl.shop.interaction.dto.response.StockResponse;
import pl.shop.interaction.service.CatalogService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CatalogService catalogService;

    @Test
    void getProducts_categoryAndPaginationGiven_returnsPage() throws Exception {
        //given
        PageResponse<ProductSummaryResponse> page = new PageResponse<>(List.of(
                new ProductSummaryResponse(5L, "Gaming PC Pro", "Computer", "MyShop",
                        new PriceResponse(new BigDecimal("4800.00"), new BigDecimal("0.23"), new BigDecimal("1104.00"), new BigDecimal("5904.00"), "PLN"), true)
        ), 0, 10, 1L, 1);
        ArgumentCaptor<String> categoryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaginationRequest> paginationCaptor = ArgumentCaptor.forClass(PaginationRequest.class);
        when(catalogService.getProducts(any(), any(PaginationRequest.class))).thenReturn(page);
        //when //then
        mockMvc.perform(get("/api/products")
                        .param("category", "Computer")
                        .param("size", "10")
                        .param("sortBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].configurable").value(true))
                .andExpect(jsonPath("$.content[0].price.grossPrice").value(5904.00));
        Mockito.verify(catalogService).getProducts(categoryCaptor.capture(), paginationCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("Computer", categoryCaptor.getValue()),
                () -> Assertions.assertEquals(10, paginationCaptor.getValue().size()),
                () -> Assertions.assertEquals("name", paginationCaptor.getValue().sortBy()),
                () -> Assertions.assertNull(paginationCaptor.getValue().page())
        );
    }

    @Test
    void getProduct_productExists_returnsProductWithStock() throws Exception {
        //given
        ProductDetailsResponse details = new ProductDetailsResponse(
                new ProductResponse(5L, "Gaming PC Pro", "Computer", "MyShop", null, true, List.of()),
                new StockResponse(5L, 10, true));
        when(catalogService.getProductDetails(5L)).thenReturn(details);
        //when //then
        mockMvc.perform(get("/api/products/{productId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.name").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.stock.quantity").value(10))
                .andExpect(jsonPath("$.stock.available").value(true));
    }

    @Test
    void getProduct_productServiceReturnsNotFound_returnsNotFoundWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8081/products/999", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":404,\"errorCode\":\"PRODUCT_NOT_FOUND\",\"message\":\"Couldn't find product with id: 999\"}".getBytes(StandardCharsets.UTF_8);
        when(catalogService.getProductDetails(999L)).thenThrow(new FeignException.NotFound("Not Found", request, body, null));
        //when //then
        mockMvc.perform(get("/api/products/{productId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999"));
    }

    @Test
    void getProduct_productServiceUnavailable_returnsServiceUnavailable() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8081/products/5", Map.of(), null, StandardCharsets.UTF_8, null);
        when(catalogService.getProductDetails(5L))
                .thenThrow(new RetryableException(503, "Service Unavailable", Request.HttpMethod.GET, (Long) null, request));
        //when //then
        mockMvc.perform(get("/api/products/{productId}", 5L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void getConfigurations_productGiven_returnsList() throws Exception {
        //given
        when(catalogService.getConfigurations(5L)).thenReturn(List.of());
        //when //then
        mockMvc.perform(get("/api/products/{productId}/configurations", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        Mockito.verify(catalogService).getConfigurations(5L);
    }

    @Test
    void quote_configurationIdsGiven_returnsQuote() throws Exception {
        //given
        QuoteResponse quote = new QuoteResponse(5L, "Gaming PC Pro",
                List.of(new QuoteComponentResponse(2L, 2L, "Graphics card", "RTX 4070")),
                new PriceResponse(new BigDecimal("6042.00"), new BigDecimal("0.23"), new BigDecimal("1389.66"), new BigDecimal("7431.66"), "PLN"),
                List.of(5L, 2L));
        ArgumentCaptor<QuoteRequest> requestCaptor = ArgumentCaptor.forClass(QuoteRequest.class);
        when(catalogService.quote(anyLong(), any(QuoteRequest.class))).thenReturn(quote);
        //when //then
        mockMvc.perform(post("/api/products/{productId}/quote", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configurationIds\":[2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice.netPrice").value(6042.00))
                .andExpect(jsonPath("$.stockProductIds", hasSize(2)));
        Mockito.verify(catalogService).quote(Mockito.eq(5L), requestCaptor.capture());
        Assertions.assertEquals(List.of(2L), requestCaptor.getValue().configurationIds());
    }

    @Test
    void quote_bodyMalformed_returnsBadRequest() throws Exception {
        //given
        String body = "{\"configurationIds\":";
        //when //then
        mockMvc.perform(post("/api/products/{productId}/quote", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
        Mockito.verify(catalogService, Mockito.never()).quote(anyLong(), any());
    }
}
