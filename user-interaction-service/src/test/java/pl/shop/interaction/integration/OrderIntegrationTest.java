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
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.OrderDetailsResponse;
import pl.shop.interaction.dto.response.OrderResponse;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class OrderIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void placeOrder_dataCorrect_exactBodyForwardedAndCreatedOrderReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/orders"))
                .withRequestBody(WireMock.equalToJson("""
                        {"cartId": 1,
                         "customer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                         "shippingAddress": {"street": "Grunwaldzka 10", "city": "Gdansk", "postalCode": "80-001", "country": "PL"}}
                        """))
                .willReturn(WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 10, "orderNumber": "order-1", "cartId": 1, "status": "PLACED",
                                 "customer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                                 "shippingAddress": {"street": "Grunwaldzka 10", "city": "Gdansk", "postalCode": "80-001", "country": "PL"},
                                 "items": [],
                                 "total": {"netPrice": 12084.00, "vatAmount": 2779.32, "grossPrice": 14863.32, "currency": "PLN"},
                                 "createdAt": "2026-11-02T12:00:00"}
                                """)));
        //when
        String json = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartId": 1,
                                 "customer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                                 "shippingAddress": {"street": "Grunwaldzka 10", "city": "Gdansk", "postalCode": "80-001", "country": "PL"}}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        OrderResponse response = objectMapper.readValue(json, OrderResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, response.id()),
                () -> Assertions.assertEquals("order-1", response.orderNumber()),
                () -> Assertions.assertEquals("PLACED", response.status()),
                () -> Assertions.assertEquals("jan@example.com", response.customer().email()),
                () -> Assertions.assertEquals("Gdansk", response.shippingAddress().city()),
                () -> Assertions.assertEquals(0, new BigDecimal("14863.32").compareTo(response.total().grossPrice())),
                () -> Assertions.assertEquals(LocalDateTime.of(2026, 11, 2, 12, 0), response.createdAt())
        );
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/orders"))
                .withRequestBody(WireMock.equalToJson("""
                        {"cartId": 1,
                         "customer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                         "shippingAddress": {"street": "Grunwaldzka 10", "city": "Gdansk", "postalCode": "80-001", "country": "PL"}}
                        """)));
    }

    @Test
    void placeOrder_orderServiceReturns503_requestSentFiveTimesAndServiceUnavailableReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/orders"))
                .willReturn(WireMock.aResponse().withStatus(503)));
        //when //then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartId": 1,
                                 "customer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                                 "shippingAddress": {"street": "Grunwaldzka 10", "city": "Gdansk", "postalCode": "80-001", "country": "PL"}}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Remote service is unavailable, try again later"));
        WireMock.verify(5, WireMock.postRequestedFor(WireMock.urlEqualTo("/orders")));
    }

    @Test
    void getOrderDetails_invoiceIssued_orderAndInvoiceMergedInOneResponse() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/orders/10"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 10, "orderNumber": "order-1", "cartId": 1, "status": "PLACED", "items": [],
                                 "total": {"netPrice": 2275.00, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                 "createdAt": "2026-11-02T12:00:00"}
                                """)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/invoices/orders/10"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 3, "invoiceNumber": "FV/2026/11/0001", "orderId": 10, "orderNumber": "order-1", "status": "ISSUED",
                                 "buyer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                                 "lines": [{"id": 1, "productName": "RTX 4070", "quantity": 1, "unitNetPrice": 2275.00, "vatRate": 0.23,
                                            "netAmount": 2275.00, "vatAmount": 523.25, "grossAmount": 2798.25}],
                                 "totalNet": 2275.00, "totalVat": 523.25, "totalGross": 2798.25, "currency": "PLN",
                                 "issuedAt": "2026-11-02T12:00:05"}
                                """)));
        //when
        String json = mockMvc.perform(get("/api/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        OrderDetailsResponse response = objectMapper.readValue(json, OrderDetailsResponse.class);
        OrderResponse order = response.order();
        InvoiceResponse invoice = response.invoice();
        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, order.id()),
                () -> Assertions.assertEquals("PLACED", order.status()),
                () -> Assertions.assertNotNull(invoice),
                () -> Assertions.assertEquals("FV/2026/11/0001", invoice.invoiceNumber()),
                () -> Assertions.assertEquals(10L, invoice.orderId()),
                () -> Assertions.assertEquals(1, invoice.lines().size()),
                () -> Assertions.assertEquals(0, new BigDecimal("2798.25").compareTo(invoice.totalGross())),
                () -> Assertions.assertEquals(LocalDateTime.of(2026, 11, 2, 12, 0, 5), invoice.issuedAt())
        );
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/orders/10")));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/invoices/orders/10")));
    }

    @Test
    void getOrderDetails_invoiceReturns404_orderReturnedWithNullInvoice() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/orders/11"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 11, "orderNumber": "order-2", "cartId": 2, "status": "PLACED", "items": [],
                                 "createdAt": "2026-11-03T09:30:00"}
                                """)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/invoices/orders/11"))
                .willReturn(WireMock.aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 404, "errorCode": "INVOICE_NOT_FOUND", "message": "Invoice not found"}
                                """)));
        //when
        String json = mockMvc.perform(get("/api/orders/{orderId}", 11L))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        //then
        OrderDetailsResponse response = objectMapper.readValue(json, OrderDetailsResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(11L, response.order().id()),
                () -> Assertions.assertEquals("order-2", response.order().orderNumber()),
                () -> Assertions.assertNull(response.invoice())
        );
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/invoices/orders/11")));
    }

    @Test
    void cancelOrder_orderExists_cancelForwardedAndCancelledOrderReturned() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/orders/10/cancel"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 10, "orderNumber": "order-1", "cartId": 1, "status": "CANCELLED", "items": [],
                                 "createdAt": "2026-11-02T12:00:00", "cancelledAt": "2026-11-02T13:00:15"}
                                """)));
        //when //then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-11-02T13:00:15"));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/orders/10/cancel")));
    }

    @Test
    void cancelOrder_orderServiceReturns500_serviceUnavailableReturnedAfterSingleAttempt() throws Exception {
        //given
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/orders/10/cancel"))
                .willReturn(WireMock.aResponse().withStatus(500)));
        //when //then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 10L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("REMOTE_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("Remote service failed, try again later"));
        WireMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/orders/10/cancel")));
    }

    @Test
    void getOrders_emailAndPaginationGiven_queryParamsForwardedAndMissingOnesOmitted() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/orders"))
                .withQueryParam("email", WireMock.equalTo("jan@example.com"))
                .withQueryParam("size", WireMock.equalTo("5"))
                .withQueryParam("direction", WireMock.equalTo("DESC"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"content": [{"id": 10, "orderNumber": "order-1", "status": "PLACED", "itemCount": 1,
                                              "grossPrice": 14863.32, "currency": "PLN", "createdAt": "2026-11-02T12:00:00"}],
                                 "pageNumber": 0, "pageSize": 5, "totalElements": 1, "totalPages": 1}
                                """)));
        //when //then
        mockMvc.perform(get("/api/orders")
                        .param("email", "jan@example.com")
                        .param("size", "5")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber").value("order-1"))
                .andExpect(jsonPath("$.pageSize").value(5));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/orders"))
                .withQueryParam("email", WireMock.equalTo("jan@example.com"))
                .withQueryParam("size", WireMock.equalTo("5"))
                .withQueryParam("direction", WireMock.equalTo("DESC"))
                .withQueryParam("page", WireMock.absent())
                .withQueryParam("sortBy", WireMock.absent()));
    }

    @Test
    void getInvoiceByOrder_invoiceServiceReturns404_notFoundPassedThrough() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/invoices/orders/11"))
                .willReturn(WireMock.aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 404, "errorCode": "INVOICE_NOT_FOUND", "message": "Invoice not found"}
                                """)));
        //when //then
        mockMvc.perform(get("/api/invoices/orders/{orderId}", 11L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("INVOICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invoice not found"));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/invoices/orders/11")));
    }

    @Test
    void getInvoices_emailAndPaginationGiven_queryParamsForwarded() throws Exception {
        //given
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/invoices"))
                .withQueryParam("email", WireMock.equalTo("jan@example.com"))
                .withQueryParam("page", WireMock.equalTo("0"))
                .withQueryParam("sortBy", WireMock.equalTo("issuedAt"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"content": [{"id": 3, "invoiceNumber": "FV/2026/11/0001", "orderNumber": "order-1", "status": "ISSUED",
                                              "totalGross": 2798.25, "currency": "PLN", "issuedAt": "2026-11-02T12:00:05"}],
                                 "pageNumber": 0, "pageSize": 20, "totalElements": 1, "totalPages": 1}
                                """)));
        //when //then
        mockMvc.perform(get("/api/invoices")
                        .param("email", "jan@example.com")
                        .param("page", "0")
                        .param("sortBy", "issuedAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("FV/2026/11/0001"))
                .andExpect(jsonPath("$.totalElements").value(1));
        WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/invoices"))
                .withQueryParam("email", WireMock.equalTo("jan@example.com"))
                .withQueryParam("page", WireMock.equalTo("0"))
                .withQueryParam("sortBy", WireMock.equalTo("issuedAt"))
                .withQueryParam("size", WireMock.absent())
                .withQueryParam("direction", WireMock.absent()));
    }
}
