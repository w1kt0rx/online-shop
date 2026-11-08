package pl.shop.interaction.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.request.DeliveryRequest;
import pl.shop.interaction.dto.response.StockResponse;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableWireMock
public class InventoryServiceClientTest {

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void getStock_serviceReturns200_stockReturnedSuccessfully() {
        //given
        stubFor(get(urlEqualTo("/inventory/5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"productId\": 5, \"quantity\": 10, \"available\": true}")));

        //when
        StockResponse response = inventoryServiceClient.getStock(5L);

        //then
        assertAll(
                () -> assertEquals(5L, response.productId()),
                () -> assertEquals(10, response.quantity()),
                () -> assertTrue(response.available())
        );
        verify(1, getRequestedFor(urlEqualTo("/inventory/5")));
    }

    @Test
    void getStock_serviceReturns404NotFound_feignExceptionThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/inventory/999"))
                .willReturn(aResponse().withStatus(404)));

        //when
        FeignException ex = assertThrows(
                FeignException.NotFound.class,
                () -> inventoryServiceClient.getStock(999L)
        );

        //then
        assertEquals(404, ex.status());
        verify(1, getRequestedFor(urlEqualTo("/inventory/999")));
    }

    @Test
    void getStocks_serviceReturns200_listReturned() {
        //given
        stubFor(get(urlEqualTo("/inventory?productIds=1&productIds=2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"productId\": 1, \"quantity\": 5, \"available\": true}, {\"productId\": 2, \"quantity\": 0, \"available\": false}]")));

        //when
        List<StockResponse> response = inventoryServiceClient.getStocks(List.of(1L, 2L));

        //then
        assertAll(
                () -> assertEquals(2, response.size()),
                () -> assertFalse(response.get(1).available())
        );
        verify(1, getRequestedFor(urlEqualTo("/inventory?productIds=1&productIds=2")));
    }

    @Test
    void registerDelivery_serviceReturns200_stockReturnedAndBodySent() {
        //given
        stubFor(post(urlEqualTo("/inventory/4/deliveries"))
                .withRequestBody(equalToJson("{\"quantity\": 5}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"productId\": 4, \"quantity\": 6, \"available\": true}")));

        //when
        StockResponse response = inventoryServiceClient.registerDelivery(4L, new DeliveryRequest(5));

        //then
        assertEquals(6, response.quantity());
        verify(1, postRequestedFor(urlEqualTo("/inventory/4/deliveries")));
    }
}
