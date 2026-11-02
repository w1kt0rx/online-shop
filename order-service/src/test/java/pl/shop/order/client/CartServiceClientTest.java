package pl.shop.order.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.order.dto.response.CartResponse;
import pl.shop.order.model.CartStatus;
import pl.shop.order.repository.OrderRepository;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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
public class CartServiceClientTest {

    @Autowired
    private CartServiceClient cartServiceClient;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        orderRepository.deleteAll();
    }

    @Test
    void getCart_serviceReturns200_cartReturnedSuccessfully() {
        //given
        stubFor(get(urlEqualTo("/carts/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "status": "ACTIVE",
                                  "items": [
                                    {
                                      "id": 100,
                                      "productId": 5,
                                      "productName": "Gaming PC Pro",
                                      "quantity": 2,
                                      "unitPrice": {"netPrice": 5855.00, "vatRate": 0.23, "vatAmount": 1346.65, "grossPrice": 7201.65, "currency": "PLN"},
                                      "totalPrice": {"netPrice": 11710.00, "vatRate": 0.23, "vatAmount": 2693.30, "grossPrice": 14403.30, "currency": "PLN"},
                                      "components": [
                                        {"configurationId": 2, "componentProductId": 2, "slot": "Graphics card", "componentName": "RTX 4070"}
                                      ]
                                    }
                                  ],
                                  "total": {"netPrice": 11710.00, "vatAmount": 2693.30, "grossPrice": 14403.30, "currency": "PLN"}
                                }
                                """)));

        //when
        CartResponse response = cartServiceClient.getCart(1L);

        //then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.id()),
                () -> assertEquals(CartStatus.ACTIVE, response.status()),
                () -> assertEquals(1, response.items().size()),
                () -> assertEquals(5L, response.items().getFirst().productId()),
                () -> assertEquals(2, response.items().getFirst().quantity()),
                () -> assertEquals(0, new BigDecimal("5855.00").compareTo(response.items().getFirst().unitPrice().netPrice())),
                () -> assertEquals(2L, response.items().getFirst().components().getFirst().componentProductId()),
                () -> assertEquals("RTX 4070", response.items().getFirst().components().getFirst().componentName())
        );
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
    }

    @Test
    void getCart_serviceReturns503Consistently_retryExhaustedAndRetryableExceptionThrown() {
        //given
        stubFor(get(urlEqualTo("/carts/1"))
                .willReturn(aResponse().withStatus(503)));

        //when
        RetryableException ex = assertThrows(
                RetryableException.class,
                () -> cartServiceClient.getCart(1L)
        );

        //then
        assertEquals(503, ex.status());
        verify(5, getRequestedFor(urlEqualTo("/carts/1")));
    }

    @Test
    void getCart_serviceReturns500InternalServerError_feignExceptionThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/carts/1"))
                .willReturn(aResponse().withStatus(500)));

        //when
        FeignException ex = assertThrows(
                FeignException.InternalServerError.class,
                () -> cartServiceClient.getCart(1L)
        );

        //then
        assertEquals(500, ex.status());
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
    }

    @Test
    void getCart_serviceReturns404NotFound_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/carts/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-02T12:00:00", "status": 404, "errorCode": "CART_NOT_FOUND", "message": "Cart with id = 999 doesn't exist"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.NotFound.class,
                () -> cartServiceClient.getCart(999L)
        );

        //then
        assertAll(
                () -> assertEquals(404, ex.status()),
                () -> assertTrue(ex.contentUTF8().contains("CART_NOT_FOUND"))
        );
        verify(1, getRequestedFor(urlEqualTo("/carts/999")));
    }
}
