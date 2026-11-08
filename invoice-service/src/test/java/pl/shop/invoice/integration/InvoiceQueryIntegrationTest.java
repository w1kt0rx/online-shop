package pl.shop.invoice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.invoice.dto.response.InvoiceResponse;
import pl.shop.invoice.kafka.OrderPlacedListener;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.model.InvoiceStatus;
import pl.shop.invoice.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InvoiceQueryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderPlacedListener orderPlacedListener;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @AfterEach
    void cleanup() {
        invoiceRepository.deleteAll();
    }

    @Test
    void getInvoice_invoiceCreatedFromEvent_returnsInvoiceWithLinesAndTotals() throws Exception {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 2, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of("Color: black"))),
                new BigDecimal("480.00"), new BigDecimal("110.40"), new BigDecimal("590.40"), "PLN");
        orderPlacedListener.onOrderPlaced(event);
        Long invoiceId = invoiceRepository.findByOrderId(10L).orElseThrow().getId();
        //when
        String content = mockMvc.perform(get("/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        //then
        InvoiceResponse response = objectMapper.readValue(content, InvoiceResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(invoiceId, response.id()),
                () -> Assertions.assertTrue(response.invoiceNumber().matches("FV/\\d{4}/\\d{2}/000001")),
                () -> Assertions.assertEquals(10L, response.orderId()),
                () -> Assertions.assertEquals("order-10", response.orderNumber()),
                () -> Assertions.assertEquals(InvoiceStatus.ISSUED, response.status()),
                () -> Assertions.assertEquals("Kowalski", response.buyer().lastName()),
                () -> Assertions.assertEquals("Gdańsk", response.billingAddress().city()),
                () -> Assertions.assertEquals(1, response.lines().size()),
                () -> Assertions.assertNotNull(response.lines().getFirst().id()),
                () -> Assertions.assertEquals("Headphones ABC", response.lines().getFirst().productName()),
                () -> Assertions.assertEquals("Color: black", response.lines().getFirst().description()),
                () -> Assertions.assertEquals(new BigDecimal("480.00"), response.lines().getFirst().netAmount()),
                () -> Assertions.assertEquals(new BigDecimal("110.40"), response.lines().getFirst().vatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.lines().getFirst().grossAmount()),
                () -> Assertions.assertEquals(new BigDecimal("480.00"), response.totalNet()),
                () -> Assertions.assertEquals(new BigDecimal("110.40"), response.totalVat()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.totalGross()),
                () -> Assertions.assertEquals("PLN", response.currency()),
                () -> Assertions.assertNotNull(response.issuedAt()),
                () -> Assertions.assertNull(response.cancelledAt())
        );
    }

    @Test
    void getInvoice_invoiceNotExists_returnsNotFoundWithErrorCode() throws Exception {
        //given
        Long invoiceId = 999999L;
        //when //then
        mockMvc.perform(get("/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("INVOICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invoice with id = 999999 doesn't exist"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getInvoiceByOrder_invoiceNotExists_returnsNotFoundWithErrorCode() throws Exception {
        //given
        Long orderId = 999999L;
        //when //then
        mockMvc.perform(get("/invoices/orders/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("INVOICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invoice for order with id = 999999 doesn't exist"));
    }

    @Test
    void getInvoice_idNotNumber_returnsBadRequestWithTypeMismatch() throws Exception {
        //given
        String invoiceId = "abc";
        //when //then
        mockMvc.perform(get("/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message", Matchers.containsString("invoiceId")));
    }

    @Test
    void getInvoices_emailInDifferentCase_returnsOnlyBuyerInvoicesPagedAndSorted() throws Exception {
        //given
        orderPlacedListener.onOrderPlaced(new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN"));
        orderPlacedListener.onOrderPlaced(new OrderPlacedEvent(11L, "order-11", LocalDateTime.of(2026, 11, 2, 13, 0),
                new OrderPlacedEvent.CustomerData("Anna", "Nowak", "anna@example.com"),
                new OrderPlacedEvent.AddressData("Krótka 1", "Gdynia", "81-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN"));
        orderPlacedListener.onOrderPlaced(new OrderPlacedEvent(12L, "order-12", LocalDateTime.of(2026, 11, 2, 14, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "Jan.Kowalski@Example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(7L, "Mouse XYZ", 2, new BigDecimal("100.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("200.00"), new BigDecimal("46.00"), new BigDecimal("246.00"), "PLN"));
        orderPlacedListener.onOrderPlaced(new OrderPlacedEvent(13L, "order-13", LocalDateTime.of(2026, 11, 2, 15, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "JAN.KOWALSKI@EXAMPLE.COM"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(8L, "Keyboard QWE", 1, new BigDecimal("300.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("300.00"), new BigDecimal("69.00"), new BigDecimal("369.00"), "PLN"));
        //when //then
        mockMvc.perform(get("/invoices")
                        .param("email", "jan.kowalski@example.COM")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "id")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber").value("order-13"))
                .andExpect(jsonPath("$.content[0].status").value("ISSUED"))
                .andExpect(jsonPath("$.content[0].totalGross").value(369.00))
                .andExpect(jsonPath("$.content[0].currency").value("PLN"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/invoices")
                        .param("email", "jan.kowalski@example.COM")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sortBy", "id")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber").value("order-12"))
                .andExpect(jsonPath("$.content[0].totalGross").value(246.00))
                .andExpect(jsonPath("$.pageNumber").value(1));
        mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.content[0].orderNumber").value("order-10"))
                .andExpect(jsonPath("$.totalElements").value(4));
        Assertions.assertEquals(4L, invoiceRepository.count());
    }
}
