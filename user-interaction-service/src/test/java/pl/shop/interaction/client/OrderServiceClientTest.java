package pl.shop.interaction.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.request.AddressRequest;
import pl.shop.interaction.dto.request.CustomerRequest;
import pl.shop.interaction.dto.request.PlaceOrderRequest;
import pl.shop.interaction.dto.response.OrderResponse;
import pl.shop.interaction.dto.response.OrderSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@EnableWireMock
public class OrderServiceClientTest {

    @Autowired
    private OrderServiceClient orderServiceClient;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void placeOrder_serviceReturns201_orderReturnedAndBodySent() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(post(urlEqualTo("/orders"))
                .withRequestBody(equalToJson("""
                        {"cartId": 1,
                         "customer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                         "shippingAddress": {"street": "Długa 5/3", "city": "Gdańsk", "postalCode": "80-001", "country": "PL"}}
                        """))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 10, "orderNumber": "order-1", "cartId": 1, "status": "PLACED", "items": [],
                                 "total": {"netPrice": 12084.00, "vatAmount": 2779.32, "grossPrice": 14863.32, "currency": "PLN"},
                                 "createdAt": "2026-11-02T12:00:00"}
                                """)));

        //when
        OrderResponse response = orderServiceClient.placeOrder(request);

        //then
        assertAll(
                () -> assertEquals(10L, response.id()),
                () -> assertEquals("PLACED", response.status()),
                () -> assertEquals(0, new BigDecimal("14863.32").compareTo(response.total().grossPrice())),
                () -> assertEquals(2026, response.createdAt().getYear())
        );
        verify(1, postRequestedFor(urlEqualTo("/orders")));
    }

    @Test
    void getOrders_emailGiven_queryParamsSentAndPageReturned() {
        //given
        stubFor(get(urlPathEqualTo("/orders"))
                .withQueryParam("email", equalTo("jan@example.com"))
                .withQueryParam("direction", equalTo("DESC"))
                .withQueryParam("page", absent())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"content": [{"id": 10, "orderNumber": "order-1", "status": "PLACED", "itemCount": 1,
                                              "grossPrice": 14863.32, "currency": "PLN", "createdAt": "2026-11-02T12:00:00"}],
                                 "pageNumber": 0, "pageSize": 20, "totalElements": 1, "totalPages": 1}
                                """)));

        //when
        PageResponse<OrderSummaryResponse> response = orderServiceClient.getOrders("jan@example.com", null, null, null, "DESC");

        //then
        assertAll(
                () -> assertEquals(1, response.content().size()),
                () -> assertEquals("order-1", response.content().getFirst().orderNumber())
        );
        verify(1, getRequestedFor(urlPathEqualTo("/orders")).withQueryParam("email", equalTo("jan@example.com")));
    }

    @Test
    void cancelOrder_serviceReturns500_feignExceptionThrownImmediately() {
        //given
        stubFor(post(urlEqualTo("/orders/10/cancel"))
                .willReturn(aResponse().withStatus(500)));

        //when
        FeignException ex = assertThrows(
                FeignException.InternalServerError.class,
                () -> orderServiceClient.cancelOrder(10L)
        );

        //then
        assertEquals(500, ex.status());
        verify(1, postRequestedFor(urlEqualTo("/orders/10/cancel")));
    }
}
