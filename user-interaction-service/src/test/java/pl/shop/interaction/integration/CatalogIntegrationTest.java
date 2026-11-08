package pl.shop.interaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.response.ProductDetailsResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.dto.response.QuoteResponse;
import pl.shop.interaction.dto.response.StockResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class CatalogIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void getProductDetails_productAndStockExist_productAndStockMergedInOneResponse() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/5"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 5, "name": "Gaming PC Pro", "category": "Computer", "vendor": "MyShop",
                                 "price": {"netPrice": 4800.00, "vatRate": 0.23, "vatAmount": 1104.00, "grossPrice": 5904.00, "currency": "PLN"},
                                 "sellableSeparately": true,
                                 "configurations": [{"id": 2, "slot": "Graphics card", "componentId": 2, "componentName": "RTX 4070",
                                                     "defaultComponent": true, "multiSelect": false}]}
                                """)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/inventory/5"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId": 5, "quantity": 10, "available": true}
                                """)));
        //when
        String json = mockMvc.perform(get("/api/products/{productId}", 5L))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        ProductDetailsResponse response = objectMapper.readValue(json, ProductDetailsResponse.class);
        ProductResponse product = response.product();
        StockResponse stock = response.stock();
        Assertions.assertAll(
                () -> Assertions.assertEquals(5L, product.id()),
                () -> Assertions.assertEquals("Gaming PC Pro", product.name()),
                () -> Assertions.assertTrue(product.sellableSeparately()),
                () -> Assertions.assertEquals(0, new BigDecimal("5904.00").compareTo(product.price().grossPrice())),
                () -> Assertions.assertEquals(1, product.configurations().size()),
                () -> Assertions.assertEquals("RTX 4070", product.configurations().getFirst().componentName()),
                () -> Assertions.assertEquals(5L, stock.productId()),
                () -> Assertions.assertEquals(10, stock.quantity()),
                () -> Assertions.assertTrue(stock.available())
        );
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/products/5")));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/inventory/5")));
    }

    @Test
    void getProductDetails_inventoryReturns404_stockZeroAndUnavailableReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/7"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 7, "name": "Office PC", "category": "Computer", "vendor": "MyShop",
                                 "price": {"netPrice": 2000.00, "vatRate": 0.23, "vatAmount": 460.00, "grossPrice": 2460.00, "currency": "PLN"},
                                 "sellableSeparately": true, "configurations": []}
                                """)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/inventory/7"))
                .willReturn(WireMock.aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 404, "errorCode": "STOCK_ITEM_NOT_FOUND", "message": "Stock item not found"}
                                """)));
        //when
        String json = mockMvc.perform(get("/api/products/{productId}", 7L))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        ProductDetailsResponse response = objectMapper.readValue(json, ProductDetailsResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals("Office PC", response.product().name()),
                () -> Assertions.assertEquals(7L, response.stock().productId()),
                () -> Assertions.assertEquals(0, response.stock().quantity()),
                () -> Assertions.assertFalse(response.stock().available())
        );
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/inventory/7")));
    }

    @Test
    void getProductDetails_productReturns404_remoteErrorPassedThroughAndInventoryNotCalled() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/999"))
                .willReturn(WireMock.aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 404, "errorCode": "PRODUCT_NOT_FOUND", "message": "Couldn't find product with id: 999"}
                                """)));
        //when //then
        mockMvc.perform(get("/api/products/{productId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Couldn't find product with id: 999"));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/products/999")));
        WireMock.verify(0, WireMock.getRequestedFor(WireMock.urlPathMatching("/inventory.*")));
    }

    @Test
    void getProductDetails_inventoryReturns503_inventoryCalledFiveTimesAndServiceUnavailableReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/products/5"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 5, "name": "Gaming PC Pro", "category": "Computer", "vendor": "MyShop",
                                 "sellableSeparately": true, "configurations": []}
                                """)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/inventory/5"))
                .willReturn(WireMock.aResponse().withStatus(503)));
        //when //then
        mockMvc.perform(get("/api/products/{productId}", 5L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Remote service is unavailable, try again later"));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/products/5")));
        WireMock.verify(5, WireMock.getRequestedFor(WireMock.urlEqualTo("/inventory/5")));
    }

    @Test
    void getProducts_categoryAndPaginationGiven_allQueryParamsForwarded() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/products"))
                .withQueryParam("category", WireMock.equalTo("Computer"))
                .withQueryParam("page", WireMock.equalTo("1"))
                .withQueryParam("size", WireMock.equalTo("5"))
                .withQueryParam("sortBy", WireMock.equalTo("name"))
                .withQueryParam("direction", WireMock.equalTo("DESC"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"content": [{"id": 5, "name": "Gaming PC Pro", "category": "Computer", "vendor": "MyShop",
                                              "price": {"netPrice": 4800.00, "vatRate": 0.23, "vatAmount": 1104.00, "grossPrice": 5904.00, "currency": "PLN"},
                                              "configurable": true}],
                                 "pageNumber": 1, "pageSize": 5, "totalElements": 6, "totalPages": 2}
                                """)));
        //when //then
        mockMvc.perform(get("/api/products")
                        .param("category", "Computer")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.content[0].configurable").value(true))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/products"))
                .withQueryParam("category", WireMock.equalTo("Computer"))
                .withQueryParam("page", WireMock.equalTo("1"))
                .withQueryParam("size", WireMock.equalTo("5"))
                .withQueryParam("sortBy", WireMock.equalTo("name"))
                .withQueryParam("direction", WireMock.equalTo("DESC")));
    }

    @Test
    void quote_configurationIdsGiven_exactBodyForwardedAndQuoteReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/products/5/configurations/quote"))
                .withRequestBody(WireMock.equalToJson("""
                        {"configurationIds": [2, 4]}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId": 5, "productName": "Gaming PC Pro",
                                 "selectedComponents": [{"configurationId": 2, "componentProductId": 2, "slot": "Graphics card", "componentName": "RTX 4070"},
                                                        {"configurationId": 4, "componentProductId": 4, "slot": "RAM", "componentName": "32GB DDR5"}],
                                 "unitPrice": {"netPrice": 6042.00, "vatRate": 0.23, "vatAmount": 1389.66, "grossPrice": 7431.66, "currency": "PLN"},
                                 "stockProductIds": [5, 2, 4]}
                                """)));
        //when
        String json = mockMvc.perform(post("/api/products/{productId}/quote", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"configurationIds": [2, 4]}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        QuoteResponse response = objectMapper.readValue(json, QuoteResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(5L, response.productId()),
                () -> Assertions.assertEquals(2, response.selectedComponents().size()),
                () -> Assertions.assertEquals("RAM", response.selectedComponents().get(1).slot()),
                () -> Assertions.assertEquals(0, new BigDecimal("7431.66").compareTo(response.unitPrice().grossPrice())),
                () -> Assertions.assertEquals(List.of(5L, 2L, 4L), response.stockProductIds())
        );
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/products/5/configurations/quote"))
                .withRequestBody(WireMock.equalToJson("""
                        {"configurationIds": [2, 4]}
                        """)));
    }

    @Test
    void getProductDetails_productIdNotNumeric_badRequestAndNoDownstreamCall() throws Exception {
        //given
        String productId = "abc";
        //when //then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: productId"));
        WireMock.verify(0, WireMock.anyRequestedFor(WireMock.anyUrl()));
    }

    @Test
    void createProduct_dataCorrect_bodyForwardedAndCreatedProductReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/products"))
                .withRequestBody(WireMock.equalToJson("""
                        {"name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA",
                         "price": {"netPrice": 2275.00, "vatRate": 0.23, "currency": "PLN"}, "sellableSeparately": true}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 2, "name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA",
                                 "price": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                 "sellableSeparately": true, "configurations": []}
                                """)));
        //when
        String json = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA",
                                 "price": {"netPrice": 2275.00, "vatRate": 0.23, "currency": "PLN"}, "sellableSeparately": true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        ProductResponse response = objectMapper.readValue(json, ProductResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(2L, response.id()),
                () -> Assertions.assertEquals("NVIDIA", response.vendor()),
                () -> Assertions.assertEquals(0, new BigDecimal("2798.25").compareTo(response.price().grossPrice())),
                () -> Assertions.assertTrue(response.configurations().isEmpty())
        );
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/products"))
                .withRequestBody(WireMock.equalToJson("""
                        {"name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA",
                         "price": {"netPrice": 2275.00, "vatRate": 0.23, "currency": "PLN"}, "sellableSeparately": true}
                        """)));
    }

    @Test
    void deleteProduct_productServiceReturns204_noContentReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.delete(WireMock.urlEqualTo("/products/6"))
                .willReturn(WireMock.aResponse().withStatus(204)));
        //when //then
        mockMvc.perform(delete("/api/admin/products/{productId}", 6L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        WireMock.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo("/products/6")));
    }

    @Test
    void deleteProduct_productServiceReturns409_conflictPassedThroughWithoutRetry() throws Exception {
        //given
        WireMock.stubFor(WireMock.delete(WireMock.urlEqualTo("/products/2"))
                .willReturn(WireMock.aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 409, "errorCode": "DATA_CONFLICT", "message": "Operation violates data constraints"}
                                """)));
        //when //then
        mockMvc.perform(delete("/api/admin/products/{productId}", 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Operation violates data constraints"));
        WireMock.verify(1, WireMock.deleteRequestedFor(WireMock.urlEqualTo("/products/2")));
    }

    @Test
    void changeQuantity_dataCorrect_putWithBodyForwardedAndStockReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.put(WireMock.urlEqualTo("/inventory/4"))
                .withRequestBody(WireMock.equalToJson("""
                        {"quantity": 15}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId": 4, "quantity": 15, "available": true}
                                """)));
        //when
        String json = mockMvc.perform(put("/api/admin/inventory/{productId}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 15}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        StockResponse response = objectMapper.readValue(json, StockResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(4L, response.productId()),
                () -> Assertions.assertEquals(15, response.quantity()),
                () -> Assertions.assertTrue(response.available())
        );
        WireMock.verify(1, WireMock.putRequestedFor(WireMock.urlEqualTo("/inventory/4"))
                .withRequestBody(WireMock.equalToJson("""
                        {"quantity": 15}
                        """)));
    }

    @Test
    void registerDelivery_dataCorrect_postWithBodyForwardedAndStockReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/inventory/4/deliveries"))
                .withRequestBody(WireMock.equalToJson("""
                        {"quantity": 5}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId": 4, "quantity": 6, "available": true}
                                """)));
        //when //then
        mockMvc.perform(post("/api/admin/inventory/{productId}/deliveries", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(4))
                .andExpect(jsonPath("$.quantity").value(6))
                .andExpect(jsonPath("$.available").value(true));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/inventory/4/deliveries"))
                .withRequestBody(WireMock.equalToJson("""
                        {"quantity": 5}
                        """)));
    }

    @Test
    void getStocks_productIdsGivenCommaSeparated_repeatedQueryParamForwarded() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/inventory?productIds=1&productIds=2"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"productId": 1, "quantity": 5, "available": true},
                                 {"productId": 2, "quantity": 0, "available": false}]
                                """)));
        //when //then
        mockMvc.perform(get("/api/admin/inventory")
                        .param("productIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].productId").value(2))
                .andExpect(jsonPath("$[1].available").value(false));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/inventory?productIds=1&productIds=2")));
    }
}
