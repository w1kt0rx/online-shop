package pl.shop.order.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.order.dto.request.StockReservationItemRequest;
import pl.shop.order.dto.request.StockReservationRequest;
import pl.shop.order.dto.response.StockResponse;
import pl.shop.order.repository.OrderRepository;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableWireMock
public class InventoryServiceClientTest {

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        orderRepository.deleteAll();
    }

    @Test
    void reserve_serviceReturns200_stockReturnedSuccessfully() {
        //given
        StockReservationRequest request = new StockReservationRequest("order-1", List.of(
                new StockReservationItemRequest(5L, 2),
                new StockReservationItemRequest(2L, 2)
        ));
        stubFor(post(urlEqualTo("/inventory/reservations"))
                .withRequestBody(equalToJson("""
                        {"orderNumber": "order-1", "items": [{"productId": 5, "quantity": 2}, {"productId": 2, "quantity": 2}]}
                        """))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 5, "quantity": 8, "available": true},
                                  {"productId": 2, "quantity": 0, "available": false}
                                ]
                                """)));

        //when
        List<StockResponse> response = inventoryServiceClient.reserve(request);

        //then
        assertAll(
                () -> assertEquals(2, response.size()),
                () -> assertEquals(8, response.get(0).quantity()),
                () -> assertTrue(response.get(0).available()),
                () -> assertEquals(2L, response.get(1).productId()),
                () -> assertFalse(response.get(1).available())
        );
        verify(1, postRequestedFor(urlEqualTo("/inventory/reservations")));
    }

    @Test
    void reserve_serviceReturns409Conflict_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        StockReservationRequest request = new StockReservationRequest("order-1", List.of(
                new StockReservationItemRequest(2L, 3)
        ));
        stubFor(post(urlEqualTo("/inventory/reservations"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-02T12:00:00", "status": 409, "errorCode": "INSUFFICIENT_STOCK", "message": "Insufficient stock for product with id = 2. Requested: 3, available: 2"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.Conflict.class,
                () -> inventoryServiceClient.reserve(request)
        );

        //then
        assertAll(
                () -> assertEquals(409, ex.status()),
                () -> assertTrue(ex.contentUTF8().contains("INSUFFICIENT_STOCK"))
        );
        verify(1, postRequestedFor(urlEqualTo("/inventory/reservations")));
    }

    @Test
    void reserve_serviceReturns503Consistently_retryExhaustedAndRetryableExceptionThrown() {
        //given
        StockReservationRequest request = new StockReservationRequest("order-1", List.of(
                new StockReservationItemRequest(2L, 1)
        ));
        stubFor(post(urlEqualTo("/inventory/reservations"))
                .willReturn(aResponse().withStatus(503)));

        //when
        RetryableException ex = assertThrows(
                RetryableException.class,
                () -> inventoryServiceClient.reserve(request)
        );

        //then
        assertEquals(503, ex.status());
        verify(5, postRequestedFor(urlEqualTo("/inventory/reservations")));
    }

    @Test
    void release_serviceReturns200_stockReturnedSuccessfully() {
        //given
        StockReservationRequest request = new StockReservationRequest("order-1", List.of(
                new StockReservationItemRequest(2L, 2)
        ));
        stubFor(post(urlEqualTo("/inventory/releases"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"productId": 2, "quantity": 2, "available": true}]
                                """)));

        //when
        List<StockResponse> response = inventoryServiceClient.release(request);

        //then
        assertAll(
                () -> assertEquals(1, response.size()),
                () -> assertEquals(2, response.getFirst().quantity())
        );
        verify(1, postRequestedFor(urlEqualTo("/inventory/releases")));
    }
}
