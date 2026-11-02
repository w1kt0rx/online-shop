package pl.shop.cart.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.cart.dto.response.StockResponse;
import pl.shop.cart.repository.CartRepository;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableWireMock
public class InventoryServiceClientTest {

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private CartRepository cartRepository;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        cartRepository.deleteAll();
    }

    @Test
    void getStock_serviceReturns200_stockReturnedSuccessfully() {
        //given
        stubFor(get(urlEqualTo("/inventory?productIds=1&productIds=4"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 1, "quantity": 5, "available": true},
                                  {"productId": 4, "quantity": 0, "available": false}
                                ]
                                """)));

        //when
        List<StockResponse> response = inventoryServiceClient.getStock(List.of(1L, 4L));

        //then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(2, response.size()),
                () -> assertEquals(1L, response.getFirst().productId()),
                () -> assertEquals(5, response.getFirst().quantity()),
                () -> assertTrue(response.getFirst().available()),
                () -> assertEquals(4L, response.get(1).productId()),
                () -> assertEquals(0, response.get(1).quantity()),
                () -> assertFalse(response.get(1).available())
        );
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1&productIds=4")));
    }

    @Test
    void getStock_serviceReturns503Consistently_retryExhaustedAndRetryableExceptionThrown() {
        //given
        stubFor(get(urlEqualTo("/inventory?productIds=1"))
                .willReturn(aResponse().withStatus(503)));

        //when
        RetryableException ex = assertThrows(
                RetryableException.class,
                () -> inventoryServiceClient.getStock(List.of(1L))
        );

        //then
        assertEquals(503, ex.status());
        verify(5, getRequestedFor(urlEqualTo("/inventory?productIds=1")));
    }

    @Test
    void getStock_serviceReturns500InternalServerError_feignExceptionThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/inventory?productIds=1"))
                .willReturn(aResponse().withStatus(500)));

        //when
        FeignException ex = assertThrows(
                FeignException.InternalServerError.class,
                () -> inventoryServiceClient.getStock(List.of(1L))
        );

        //then
        assertEquals(500, ex.status());
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1")));
    }

    @Test
    void getStock_serviceReturns400BadRequest_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/inventory?productIds=1"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-01T12:00:00", "status": 400, "errorCode": "TYPE_MISMATCH", "message": "Invalid value for parameter: productIds"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.BadRequest.class,
                () -> inventoryServiceClient.getStock(List.of(1L))
        );

        //then
        assertAll(
                () -> assertEquals(400, ex.status()),
                () -> assertTrue(ex.contentUTF8().contains("TYPE_MISMATCH"))
        );
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1")));
    }

    @Test
    void getStock_serviceReturns404NotFound_feignExceptionThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/inventory?productIds=1"))
                .willReturn(aResponse().withStatus(404)));

        //when
        FeignException ex = assertThrows(
                FeignException.NotFound.class,
                () -> inventoryServiceClient.getStock(List.of(1L))
        );

        //then
        assertEquals(404, ex.status());
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1")));
    }
}