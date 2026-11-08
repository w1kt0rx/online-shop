package pl.shop.interaction.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.request.AddCartItemRequest;
import pl.shop.interaction.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.interaction.dto.response.CartResponse;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableWireMock
public class CartServiceClientTest {

    @Autowired
    private CartServiceClient cartServiceClient;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void createCart_serviceReturns201_cartReturned() {
        //given
        stubFor(post(urlEqualTo("/carts"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 1, "status": "ACTIVE", "items": [],
                                 "total": {"netPrice": 0, "vatAmount": 0, "grossPrice": 0, "currency": "PLN"}}
                                """)));

        //when
        CartResponse response = cartServiceClient.createCart();

        //then
        assertAll(
                () -> assertEquals(1L, response.id()),
                () -> assertEquals("ACTIVE", response.status()),
                () -> assertTrue(response.items().isEmpty())
        );
        verify(1, postRequestedFor(urlEqualTo("/carts")));
    }

    @Test
    void addItem_serviceReturns409Conflict_feignExceptionWithRemoteBodyThrownImmediately() {
        //given
        stubFor(post(urlEqualTo("/carts/1/items"))
                .withRequestBody(equalToJson("{\"productId\": 5, \"configurationIds\": [2, 4], \"quantity\": 2}"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 409, "errorCode": "INSUFFICIENT_STOCK", "message": "Insufficient stock for product with id = 4. Required: 2, available: 1"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.Conflict.class,
                () -> cartServiceClient.addItem(1L, new AddCartItemRequest(5L, List.of(2L, 4L), 2))
        );

        //then
        assertTrue(ex.contentUTF8().contains("INSUFFICIENT_STOCK"));
        verify(1, postRequestedFor(urlEqualTo("/carts/1/items")));
    }

    @Test
    void changeItemQuantity_serviceReturns200_patchSentAndCartReturned() {
        //given
        stubFor(patch(urlEqualTo("/carts/1/items/100"))
                .withRequestBody(equalToJson("{\"quantity\": 3}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"status\": \"ACTIVE\", \"items\": []}")));

        //when
        CartResponse response = cartServiceClient.changeItemQuantity(1L, 100L, new ChangeCartItemQuantityRequest(3));

        //then
        assertEquals(1L, response.id());
        verify(1, patchRequestedFor(urlEqualTo("/carts/1/items/100")));
    }

    @Test
    void createCart_serviceReturns503Consistently_retryExhaustedAndRetryableExceptionThrown() {
        //given
        stubFor(post(urlEqualTo("/carts"))
                .willReturn(aResponse().withStatus(503)));

        //when
        RetryableException ex = assertThrows(
                RetryableException.class,
                () -> cartServiceClient.createCart()
        );

        //then
        assertEquals(503, ex.status());
        verify(5, postRequestedFor(urlEqualTo("/carts")));
    }
}
