package pl.shop.interaction.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.request.CreateProductRequest;
import pl.shop.interaction.dto.request.PriceRequest;
import pl.shop.interaction.dto.request.QuoteRequest;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.dto.response.ProductSummaryResponse;
import pl.shop.interaction.dto.response.QuoteResponse;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableWireMock
public class ProductServiceClientTest {

    @Autowired
    private ProductServiceClient productServiceClient;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void getProducts_serviceReturns200_pageReturnedAndOnlyGivenParamsSent() {
        //given
        stubFor(get(urlEqualTo("/products?category=Electronics&size=10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "content": [
                                    {"id": 2, "name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA",
                                     "price": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                     "configurable": false}
                                  ],
                                  "pageNumber": 0, "pageSize": 10, "totalElements": 1, "totalPages": 1
                                }
                                """)));

        //when
        PageResponse<ProductSummaryResponse> response = productServiceClient.getProducts("Electronics", null, 10, null, null);

        //then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1, response.content().size()),
                () -> assertEquals("RTX 4070", response.content().getFirst().name()),
                () -> assertEquals(0, new BigDecimal("2798.25").compareTo(response.content().getFirst().price().grossPrice())),
                () -> assertEquals(1L, response.totalElements())
        );
        verify(1, getRequestedFor(urlEqualTo("/products?category=Electronics&size=10")));
    }

    @Test
    void getProduct_serviceReturns404NotFound_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/products/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 404, "errorCode": "PRODUCT_NOT_FOUND", "message": "Couldn't find product with id: 999"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.NotFound.class,
                () -> productServiceClient.getProduct(999L)
        );

        //then
        assertTrue(ex.contentUTF8().contains("PRODUCT_NOT_FOUND"));
        verify(1, getRequestedFor(urlEqualTo("/products/999")));
    }

    @Test
    void getProduct_serviceReturns503Consistently_retryExhaustedAndRetryableExceptionThrown() {
        //given
        stubFor(get(urlEqualTo("/products/5"))
                .willReturn(aResponse().withStatus(503)));

        //when
        RetryableException ex = assertThrows(
                RetryableException.class,
                () -> productServiceClient.getProduct(5L)
        );

        //then
        assertEquals(503, ex.status());
        verify(5, getRequestedFor(urlEqualTo("/products/5")));
    }

    @Test
    void createProduct_serviceReturns201_productReturnedAndBodySent() {
        //given
        CreateProductRequest request = new CreateProductRequest("RTX 4070", "Electronics", "NVIDIA",
                new PriceRequest(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        stubFor(post(urlEqualTo("/products"))
                .withRequestBody(equalToJson("""
                        {"name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA",
                         "price": {"netPrice": 2275.00, "vatRate": 0.23, "currency": "PLN"}, "sellableSeparately": true}
                        """))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 2, "name": "RTX 4070", "category": "Electronics", "vendor": "NVIDIA", "sellableSeparately": true, "configurations": []}
                                """)));

        //when
        ProductResponse response = productServiceClient.createProduct(request);

        //then
        assertAll(
                () -> assertEquals(2L, response.id()),
                () -> assertTrue(response.configurations().isEmpty())
        );
        verify(1, postRequestedFor(urlEqualTo("/products")));
    }

    @Test
    void deleteProduct_serviceReturns204_noExceptionThrown() {
        //given
        stubFor(delete(urlEqualTo("/products/6"))
                .willReturn(aResponse().withStatus(204)));

        //when
        productServiceClient.deleteProduct(6L);

        //then
        verify(1, deleteRequestedFor(urlEqualTo("/products/6")));
    }

    @Test
    void quote_serviceReturns200_quoteReturned() {
        //given
        stubFor(post(urlEqualTo("/products/5/configurations/quote"))
                .withRequestBody(equalToJson("{\"configurationIds\":[2,4]}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId": 5, "productName": "Gaming PC Pro", "selectedComponents": [],
                                 "unitPrice": {"netPrice": 6042.00, "vatRate": 0.23, "vatAmount": 1389.66, "grossPrice": 7431.66, "currency": "PLN"},
                                 "stockProductIds": [5, 2, 4]}
                                """)));

        //when
        QuoteResponse response = productServiceClient.quote(5L, new QuoteRequest(List.of(2L, 4L)));

        //then
        assertAll(
                () -> assertEquals(List.of(5L, 2L, 4L), response.stockProductIds()),
                () -> assertEquals(0, new BigDecimal("6042.00").compareTo(response.unitPrice().netPrice()))
        );
        verify(1, postRequestedFor(urlEqualTo("/products/5/configurations/quote")));
    }
}
