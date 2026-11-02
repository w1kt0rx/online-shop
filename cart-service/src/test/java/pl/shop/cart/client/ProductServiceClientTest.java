package pl.shop.cart.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.cart.dto.request.QuoteRequest;
import pl.shop.cart.dto.response.QuoteResponse;
import pl.shop.cart.repository.CartRepository;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
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

    @Autowired
    private CartRepository cartRepository;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        cartRepository.deleteAll();
    }

    @Test
    void quote_serviceReturns200_quoteReturnedSuccessfully() {
        //given
        stubFor(post(urlEqualTo("/products/1/configurations/quote"))
                .withRequestBody(equalToJson("{\"configurationIds\":[11]}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "productId": 1,
                                  "productName": "Gaming PC Pro",
                                  "selectedComponents": [
                                    {"configurationId": 11, "componentProductId": 4, "slot": "Graphics card", "componentName": "RTX 4070"}
                                  ],
                                  "unitPrice": {"netPrice": 5855.00, "vatRate": 0.23, "vatAmount": 1346.65, "grossPrice": 7201.65, "currency": "PLN"},
                                  "stockProductIds": [1, 4]
                                }
                                """)));

        //when
        QuoteResponse response = productServiceClient.quote(1L, new QuoteRequest(List.of(11L)));

        //then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.productId()),
                () -> assertEquals("Gaming PC Pro", response.productName()),
                () -> assertEquals(1, response.selectedComponents().size()),
                () -> assertEquals(11L, response.selectedComponents().getFirst().configurationId()),
                () -> assertEquals(4L, response.selectedComponents().getFirst().componentProductId()),
                () -> assertEquals("Graphics card", response.selectedComponents().getFirst().slot()),
                () -> assertEquals("RTX 4070", response.selectedComponents().getFirst().componentName()),
                () -> assertEquals(0, new BigDecimal("5855.00").compareTo(response.unitPrice().netPrice())),
                () -> assertEquals(0, new BigDecimal("7201.65").compareTo(response.unitPrice().grossPrice())),
                () -> assertEquals("PLN", response.unitPrice().currency()),
                () -> assertEquals(List.of(1L, 4L), response.stockProductIds())
        );
        verify(1, postRequestedFor(urlEqualTo("/products/1/configurations/quote")));
    }

    @Test
    void quote_serviceReturns503Consistently_retryExhaustedAndRetryableExceptionThrown() {
        //given
        stubFor(post(urlEqualTo("/products/1/configurations/quote"))
                .willReturn(aResponse().withStatus(503)));

        //when
        RetryableException ex = assertThrows(
                RetryableException.class,
                () -> productServiceClient.quote(1L, new QuoteRequest(List.of()))
        );

        //then
        assertEquals(503, ex.status());
        verify(5, postRequestedFor(urlEqualTo("/products/1/configurations/quote")));
    }

    @Test
    void quote_serviceReturns500InternalServerError_feignExceptionThrownImmediately() {
        //given
        stubFor(post(urlEqualTo("/products/1/configurations/quote"))
                .willReturn(aResponse().withStatus(500)));

        //when
        FeignException ex = assertThrows(
                FeignException.InternalServerError.class,
                () -> productServiceClient.quote(1L, new QuoteRequest(List.of()))
        );

        //then
        assertEquals(500, ex.status());
        verify(1, postRequestedFor(urlEqualTo("/products/1/configurations/quote")));
    }

    @Test
    void quote_serviceReturns404NotFound_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        stubFor(post(urlEqualTo("/products/999/configurations/quote"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-01T12:00:00", "status": 404, "errorCode": "PRODUCT_NOT_FOUND", "message": "Couldn't find product with id: 999"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.NotFound.class,
                () -> productServiceClient.quote(999L, new QuoteRequest(List.of()))
        );

        //then
        assertAll(
                () -> assertEquals(404, ex.status()),
                () -> assertTrue(ex.contentUTF8().contains("PRODUCT_NOT_FOUND"))
        );
        verify(1, postRequestedFor(urlEqualTo("/products/999/configurations/quote")));
    }

    @Test
    void quote_serviceReturns400InvalidConfiguration_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        stubFor(post(urlEqualTo("/products/1/configurations/quote"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-01T12:00:00", "status": 400, "errorCode": "INVALID_CONFIGURATION", "message": "Configurations [99] do not belong to product id = 1"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.BadRequest.class,
                () -> productServiceClient.quote(1L, new QuoteRequest(List.of(99L)))
        );

        //then
        assertAll(
                () -> assertEquals(400, ex.status()),
                () -> assertTrue(ex.contentUTF8().contains("INVALID_CONFIGURATION"))
        );
        verify(1, postRequestedFor(urlEqualTo("/products/1/configurations/quote")));
    }
}