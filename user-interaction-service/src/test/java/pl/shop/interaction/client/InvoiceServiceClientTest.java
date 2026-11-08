package pl.shop.interaction.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import pl.shop.interaction.dto.response.InvoiceResponse;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnableWireMock
public class InvoiceServiceClientTest {

    @Autowired
    private InvoiceServiceClient invoiceServiceClient;

    @AfterEach
    void tearDown() {
        WireMock.reset();
    }

    @Test
    void getInvoiceByOrder_serviceReturns200_invoiceReturned() {
        //given
        stubFor(get(urlEqualTo("/invoices/orders/10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 1, "invoiceNumber": "FV/2026/11/000001", "orderId": 10, "orderNumber": "order-1", "status": "ISSUED",
                                 "buyer": {"firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com"},
                                 "billingAddress": {"street": "Długa 5/3", "city": "Gdańsk", "postalCode": "80-001", "country": "PL"},
                                 "lines": [{"id": 100, "productName": "Gaming PC Pro", "description": "Graphics card: RTX 4070", "quantity": 2,
                                            "unitNetPrice": 6042.00, "vatRate": 0.23, "netAmount": 12084.00, "vatAmount": 2779.32, "grossAmount": 14863.32}],
                                 "totalNet": 12084.00, "totalVat": 2779.32, "totalGross": 14863.32, "currency": "PLN",
                                 "issuedAt": "2026-11-02T12:00:01", "cancelledAt": null}
                                """)));

        //when
        InvoiceResponse response = invoiceServiceClient.getInvoiceByOrder(10L);

        //then
        assertAll(
                () -> assertEquals("FV/2026/11/000001", response.invoiceNumber()),
                () -> assertEquals("ISSUED", response.status()),
                () -> assertEquals("jan@example.com", response.buyer().email()),
                () -> assertEquals(1, response.lines().size()),
                () -> assertEquals(0, new BigDecimal("14863.32").compareTo(response.totalGross()))
        );
        verify(1, getRequestedFor(urlEqualTo("/invoices/orders/10")));
    }

    @Test
    void getInvoiceByOrder_serviceReturns404NotFound_feignExceptionThrownImmediately() {
        //given
        stubFor(get(urlEqualTo("/invoices/orders/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": 404, "errorCode": "INVOICE_NOT_FOUND", "message": "Invoice for order with id = 999 doesn't exist"}
                                """)));

        //when
        FeignException ex = assertThrows(
                FeignException.NotFound.class,
                () -> invoiceServiceClient.getInvoiceByOrder(999L)
        );

        //then
        assertTrue(ex.contentUTF8().contains("INVOICE_NOT_FOUND"));
        verify(1, getRequestedFor(urlEqualTo("/invoices/orders/999")));
    }
}
