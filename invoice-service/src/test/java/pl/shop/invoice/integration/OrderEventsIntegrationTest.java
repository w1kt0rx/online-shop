package pl.shop.invoice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.invoice.dto.response.InvoiceResponse;
import pl.shop.invoice.exception.InvalidInvoiceLineException;
import pl.shop.invoice.exception.InvoiceNotFoundException;
import pl.shop.invoice.kafka.OrderCancelledListener;
import pl.shop.invoice.kafka.OrderPlacedListener;
import pl.shop.invoice.kafka.event.OrderCancelledEvent;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.model.InvoiceStatus;
import pl.shop.invoice.repository.InvoiceRepository;
import pl.shop.invoice.service.InvoiceNumberGenerator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderEventsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderPlacedListener orderPlacedListener;
    @Autowired
    private OrderCancelledListener orderCancelledListener;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @MockitoSpyBean
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @AfterEach
    void cleanup() {
        invoiceRepository.deleteAll();
    }

    @Test
    void onOrderPlaced_newOrder_invoiceIssuedWithNumberBuyerAddressLinesAndTotals() throws Exception {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(
                        new OrderPlacedEvent.OrderItemData(5L, "Gaming PC Pro", 2, new BigDecimal("6042.00"), new BigDecimal("0.23"), "PLN",
                                List.of("Graphics card: RTX 4070", "RAM: 32 GB DDR5")),
                        new OrderPlacedEvent.OrderItemData(6L, "USB cable", 3, new BigDecimal("19.985"), new BigDecimal("0.08"), "PLN",
                                List.of())
                ),
                new BigDecimal("12143.97"), new BigDecimal("2784.12"), new BigDecimal("14928.09"), "PLN");
        ArgumentCaptor<LocalDateTime> issuedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        //when
        orderPlacedListener.onOrderPlaced(event);
        //then
        Mockito.verify(invoiceNumberGenerator).next(issuedAtCaptor.capture());
        LocalDateTime issuedAt = issuedAtCaptor.getValue();
        String content = mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        InvoiceResponse response = objectMapper.readValue(content, InvoiceResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, invoiceRepository.count()),
                () -> Assertions.assertNotNull(response.id()),
                () -> Assertions.assertEquals(String.format("FV/%d/%02d/%06d", issuedAt.getYear(), issuedAt.getMonthValue(), 1),
                        response.invoiceNumber()),
                () -> Assertions.assertEquals(YearMonth.from(issuedAt), YearMonth.from(response.issuedAt())),
                () -> Assertions.assertEquals(10L, response.orderId()),
                () -> Assertions.assertEquals("order-10", response.orderNumber()),
                () -> Assertions.assertEquals(InvoiceStatus.ISSUED, response.status()),
                () -> Assertions.assertNull(response.cancelledAt()),
                () -> Assertions.assertEquals("PLN", response.currency()),
                () -> Assertions.assertEquals("Jan", response.buyer().firstName()),
                () -> Assertions.assertEquals("Kowalski", response.buyer().lastName()),
                () -> Assertions.assertEquals("jan@example.com", response.buyer().email()),
                () -> Assertions.assertEquals("Długa 5/3", response.billingAddress().street()),
                () -> Assertions.assertEquals("Gdańsk", response.billingAddress().city()),
                () -> Assertions.assertEquals("80-001", response.billingAddress().postalCode()),
                () -> Assertions.assertEquals("PL", response.billingAddress().country()),
                () -> Assertions.assertEquals(2, response.lines().size()),
                () -> Assertions.assertEquals("Gaming PC Pro", response.lines().get(0).productName()),
                () -> Assertions.assertEquals("Graphics card: RTX 4070, RAM: 32 GB DDR5", response.lines().get(0).description()),
                () -> Assertions.assertEquals(2, response.lines().get(0).quantity()),
                () -> Assertions.assertEquals(new BigDecimal("6042.00"), response.lines().get(0).unitNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), response.lines().get(0).vatRate()),
                () -> Assertions.assertEquals(new BigDecimal("12084.00"), response.lines().get(0).netAmount()),
                () -> Assertions.assertEquals(new BigDecimal("2779.32"), response.lines().get(0).vatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("14863.32"), response.lines().get(0).grossAmount()),
                () -> Assertions.assertEquals("USB cable", response.lines().get(1).productName()),
                () -> Assertions.assertNull(response.lines().get(1).description()),
                () -> Assertions.assertEquals(3, response.lines().get(1).quantity()),
                () -> Assertions.assertEquals(new BigDecimal("19.99"), response.lines().get(1).unitNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.08"), response.lines().get(1).vatRate()),
                () -> Assertions.assertEquals(new BigDecimal("59.97"), response.lines().get(1).netAmount()),
                () -> Assertions.assertEquals(new BigDecimal("4.80"), response.lines().get(1).vatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("64.77"), response.lines().get(1).grossAmount()),
                () -> Assertions.assertEquals(new BigDecimal("12143.97"), response.totalNet()),
                () -> Assertions.assertEquals(new BigDecimal("2784.12"), response.totalVat()),
                () -> Assertions.assertEquals(new BigDecimal("14928.09"), response.totalGross())
        );
    }

    @Test
    void onOrderPlaced_secondOrderInSameMonth_nextSequentialNumberIssued() throws Exception {
        //given
        OrderPlacedEvent firstEvent = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN");
        OrderPlacedEvent secondEvent = new OrderPlacedEvent(11L, "order-11", LocalDateTime.of(2026, 11, 2, 13, 0),
                new OrderPlacedEvent.CustomerData("Anna", "Nowak", "anna@example.com"),
                new OrderPlacedEvent.AddressData("Krótka 1", "Gdynia", "81-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN");
        ArgumentCaptor<LocalDateTime> issuedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        //when
        orderPlacedListener.onOrderPlaced(firstEvent);
        orderPlacedListener.onOrderPlaced(secondEvent);
        //then
        Mockito.verify(invoiceNumberGenerator, Mockito.times(2)).next(issuedAtCaptor.capture());
        LocalDateTime firstIssuedAt = issuedAtCaptor.getAllValues().get(0);
        LocalDateTime secondIssuedAt = issuedAtCaptor.getAllValues().get(1);
        long expectedSecondSequence = YearMonth.from(firstIssuedAt).equals(YearMonth.from(secondIssuedAt)) ? 2L : 1L;
        mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(
                        String.format("FV/%d/%02d/%06d", firstIssuedAt.getYear(), firstIssuedAt.getMonthValue(), 1)));
        mockMvc.perform(get("/invoices/orders/{orderId}", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(
                        String.format("FV/%d/%02d/%06d", secondIssuedAt.getYear(), secondIssuedAt.getMonthValue(), expectedSecondSequence)));
        Assertions.assertEquals(2L, invoiceRepository.count());
    }

    @Test
    void onOrderPlaced_duplicateEventForSameOrder_secondInvoiceNotCreated() throws Exception {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN");
        OrderPlacedEvent duplicatedEvent = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Anna", "Nowak", "anna@example.com"),
                new OrderPlacedEvent.AddressData("Krótka 1", "Gdynia", "81-001", "PL"),
                List.of(
                        new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                                List.of()),
                        new OrderPlacedEvent.OrderItemData(7L, "Mouse XYZ", 1, new BigDecimal("100.00"), new BigDecimal("0.23"), "PLN",
                                List.of())
                ),
                new BigDecimal("340.00"), new BigDecimal("78.20"), new BigDecimal("418.20"), "PLN");
        orderPlacedListener.onOrderPlaced(event);
        //when
        orderPlacedListener.onOrderPlaced(duplicatedEvent);
        //then
        Mockito.verify(invoiceNumberGenerator, Mockito.times(1)).next(any());
        String content = mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        InvoiceResponse response = objectMapper.readValue(content, InvoiceResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, invoiceRepository.count()),
                () -> Assertions.assertTrue(response.invoiceNumber().endsWith("/000001")),
                () -> Assertions.assertEquals("Jan", response.buyer().firstName()),
                () -> Assertions.assertEquals("jan@example.com", response.buyer().email()),
                () -> Assertions.assertEquals("Gdańsk", response.billingAddress().city()),
                () -> Assertions.assertEquals(1, response.lines().size()),
                () -> Assertions.assertEquals(new BigDecimal("295.20"), response.totalGross())
        );
    }

    @Test
    void onOrderPlaced_vatRateAboveOne_throwsInvalidInvoiceLineExceptionAndNothingSaved() throws Exception {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("1.50"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("360.00"), new BigDecimal("600.00"), "PLN");
        //when //then
        InvalidInvoiceLineException ex = Assertions.assertThrows(
                InvalidInvoiceLineException.class,
                () -> orderPlacedListener.onOrderPlaced(event)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_INVOICE_LINE", ex.getErrorCode()),
                () -> Assertions.assertEquals("Vat rate has to be in range 0 - 1. Given: 1.50", ex.getMessage()),
                () -> Assertions.assertEquals(0L, invoiceRepository.count())
        );
        mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVOICE_NOT_FOUND"));
    }

    @Test
    void onOrderCancelled_invoiceIssued_invoiceCancelledWithEventDate() throws Exception {
        //given
        OrderPlacedEvent placedEvent = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 2, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("480.00"), new BigDecimal("110.40"), new BigDecimal("590.40"), "PLN");
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 11, 3, 9, 0);
        orderPlacedListener.onOrderPlaced(placedEvent);
        String invoiceNumber = invoiceRepository.findByOrderId(10L).orElseThrow().getInvoiceNumber();
        //when
        orderCancelledListener.onOrderCancelled(new OrderCancelledEvent(10L, "order-10", cancelledAt));
        //then
        String content = mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        InvoiceResponse response = objectMapper.readValue(content, InvoiceResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(InvoiceStatus.CANCELLED, response.status()),
                () -> Assertions.assertEquals(cancelledAt, response.cancelledAt()),
                () -> Assertions.assertEquals(invoiceNumber, response.invoiceNumber()),
                () -> Assertions.assertNotNull(response.issuedAt()),
                () -> Assertions.assertEquals(1, response.lines().size()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.totalGross()),
                () -> Assertions.assertEquals(InvoiceStatus.CANCELLED, invoiceRepository.findByOrderId(10L).orElseThrow().getStatus())
        );
    }

    @Test
    void onOrderCancelled_invoiceAlreadyCancelled_noExceptionAndCancelledAtUnchanged() throws Exception {
        //given
        OrderPlacedEvent placedEvent = new OrderPlacedEvent(10L, "order-10", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                        List.of())),
                new BigDecimal("240.00"), new BigDecimal("55.20"), new BigDecimal("295.20"), "PLN");
        LocalDateTime firstCancel = LocalDateTime.of(2026, 11, 3, 9, 0);
        orderPlacedListener.onOrderPlaced(placedEvent);
        orderCancelledListener.onOrderCancelled(new OrderCancelledEvent(10L, "order-10", firstCancel));
        //when
        Assertions.assertDoesNotThrow(
                () -> orderCancelledListener.onOrderCancelled(new OrderCancelledEvent(10L, "order-10", LocalDateTime.of(2026, 11, 4, 9, 0)))
        );
        //then
        String content = mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        InvoiceResponse response = objectMapper.readValue(content, InvoiceResponse.class);
        Assertions.assertAll(
                () -> Assertions.assertEquals(InvoiceStatus.CANCELLED, response.status()),
                () -> Assertions.assertEquals(firstCancel, response.cancelledAt()),
                () -> Assertions.assertEquals(1L, invoiceRepository.count()),
                () -> Assertions.assertEquals(firstCancel, invoiceRepository.findByOrderId(10L).orElseThrow().getCancelledAt())
        );
    }

    @Test
    void onOrderCancelled_invoiceNotExists_throwsInvoiceNotFoundException() {
        //given
        OrderCancelledEvent event = new OrderCancelledEvent(999L, "order-999", LocalDateTime.of(2026, 11, 3, 9, 0));
        //when //then
        InvoiceNotFoundException ex = Assertions.assertThrows(
                InvoiceNotFoundException.class,
                () -> orderCancelledListener.onOrderCancelled(event)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVOICE_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus()),
                () -> Assertions.assertEquals("Invoice for order with id = 999 doesn't exist", ex.getMessage()),
                () -> Assertions.assertEquals(0L, invoiceRepository.count())
        );
        Mockito.verifyNoInteractions(invoiceNumberGenerator);
    }
}
