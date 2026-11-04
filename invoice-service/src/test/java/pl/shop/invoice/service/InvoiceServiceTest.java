package pl.shop.invoice.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.invoice.dto.request.PaginationRequest;
import pl.shop.invoice.dto.response.InvoiceResponse;
import pl.shop.invoice.dto.response.InvoiceSummaryResponse;
import pl.shop.invoice.dto.response.PageResponse;
import pl.shop.invoice.exception.InvoiceNotFoundException;
import pl.shop.invoice.kafka.event.OrderCancelledEvent;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.mapper.InvoiceMapper;
import pl.shop.invoice.model.Address;
import pl.shop.invoice.model.Buyer;
import pl.shop.invoice.model.Invoice;
import pl.shop.invoice.model.InvoiceStatus;
import pl.shop.invoice.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InvoiceServiceTest {
    private InvoiceService invoiceService;
    private InvoiceRepository invoiceRepository;
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @BeforeEach
    void setup() {
        this.invoiceRepository = Mockito.mock(InvoiceRepository.class);
        this.invoiceNumberGenerator = Mockito.mock(InvoiceNumberGenerator.class);
        InvoiceMapper invoiceMapper = Mappers.getMapper(InvoiceMapper.class);
        this.invoiceService = new InvoiceService(invoiceRepository, invoiceMapper, invoiceNumberGenerator);
    }

    @Test
    void createInvoice_newOrder_invoiceWithLinesSaved() {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-1", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(
                        new OrderPlacedEvent.OrderItemData(5L, "Gaming PC Pro", 2, new BigDecimal("6042.00"), new BigDecimal("0.23"), "PLN",
                                List.of("Graphics card: RTX 4070", "RAM: RAM 32 GB DDR5")),
                        new OrderPlacedEvent.OrderItemData(6L, "Headphones ABC", 1, new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN",
                                List.of())
                ),
                new BigDecimal("12324.00"), new BigDecimal("2834.52"), new BigDecimal("15158.52"), "PLN");
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.existsByOrderId(10L)).thenReturn(false);
        when(invoiceNumberGenerator.next(any())).thenReturn("FV/2026/11/000001");
        //when
        invoiceService.createInvoice(event);
        //then
        Mockito.verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice invoice = invoiceCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals("FV/2026/11/000001", invoice.getInvoiceNumber()),
                () -> Assertions.assertEquals(10L, invoice.getOrderId()),
                () -> Assertions.assertEquals("order-1", invoice.getOrderNumber()),
                () -> Assertions.assertEquals(InvoiceStatus.ISSUED, invoice.getStatus()),
                () -> Assertions.assertEquals("Kowalski", invoice.getBuyer().getLastName()),
                () -> Assertions.assertEquals("jan@example.com", invoice.getBuyer().getEmail()),
                () -> Assertions.assertEquals("80-001", invoice.getBillingAddress().getPostalCode()),
                () -> Assertions.assertEquals("PLN", invoice.getCurrency()),
                () -> Assertions.assertNotNull(invoice.getIssuedAt()),
                () -> Assertions.assertEquals(2, invoice.getLines().size()),
                () -> Assertions.assertEquals("Graphics card: RTX 4070, RAM: RAM 32 GB DDR5", invoice.getLines().get(0).getDescription()),
                () -> Assertions.assertNull(invoice.getLines().get(1).getDescription()),
                () -> Assertions.assertEquals(new BigDecimal("12324.00"), invoice.getTotalNet()),
                () -> Assertions.assertEquals(new BigDecimal("2834.52"), invoice.getTotalVat()),
                () -> Assertions.assertEquals(new BigDecimal("15158.52"), invoice.getTotalGross())
        );
    }

    @Test
    void createInvoice_invoiceForOrderExists_nothingSavedAndNoNumberGenerated() {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-1", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN");
        when(invoiceRepository.existsByOrderId(10L)).thenReturn(true);
        //when
        invoiceService.createInvoice(event);
        //then
        Mockito.verify(invoiceRepository, Mockito.never()).save(any());
        Mockito.verifyNoInteractions(invoiceNumberGenerator);
    }

    @Test
    void cancelInvoice_invoiceIssued_statusCancelledWithEventDate() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 11, 3, 9, 0);
        when(invoiceRepository.findByOrderId(10L)).thenReturn(Optional.of(invoice));
        //when
        invoiceService.cancelInvoice(new OrderCancelledEvent(10L, "order-1", cancelledAt));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus()),
                () -> Assertions.assertEquals(cancelledAt, invoice.getCancelledAt())
        );
    }

    @Test
    void cancelInvoice_invoiceAlreadyCancelled_noExceptionAndDateUnchanged() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        LocalDateTime firstCancel = LocalDateTime.of(2026, 11, 3, 9, 0);
        invoice.cancel(firstCancel);
        when(invoiceRepository.findByOrderId(10L)).thenReturn(Optional.of(invoice));
        //when
        Assertions.assertDoesNotThrow(
                () -> invoiceService.cancelInvoice(new OrderCancelledEvent(10L, "order-1", LocalDateTime.of(2026, 11, 4, 9, 0)))
        );
        //then
        Assertions.assertEquals(firstCancel, invoice.getCancelledAt());
    }

    @Test
    void cancelInvoice_invoiceNotExists_throwsInvoiceNotFoundException() {
        //given
        when(invoiceRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        //when //then
        InvoiceNotFoundException ex = Assertions.assertThrows(
                InvoiceNotFoundException.class,
                () -> invoiceService.cancelInvoice(new OrderCancelledEvent(10L, "order-1", LocalDateTime.of(2026, 11, 3, 9, 0)))
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVOICE_NOT_FOUND", ex.getErrorCode()),
                () -> Assertions.assertEquals("Invoice for order with id = 10 doesn't exist", ex.getMessage())
        );
    }

    @Test
    void getInvoice_invoiceExists_returnsInvoiceResponse() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        ReflectionTestUtils.setField(invoice, "id", 1L);
        invoice.addLine("Headphones ABC", null, 2, new BigDecimal("240.00"), new BigDecimal("0.23"));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        //when
        InvoiceResponse response = invoiceService.getInvoice(1L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, response.id()),
                () -> Assertions.assertEquals("FV/2026/11/000001", response.invoiceNumber()),
                () -> Assertions.assertEquals(InvoiceStatus.ISSUED, response.status()),
                () -> Assertions.assertEquals("Jan", response.buyer().firstName()),
                () -> Assertions.assertEquals("Gdańsk", response.billingAddress().city()),
                () -> Assertions.assertEquals(1, response.lines().size()),
                () -> Assertions.assertEquals(new BigDecimal("480.00"), response.lines().getFirst().netAmount()),
                () -> Assertions.assertEquals(new BigDecimal("110.40"), response.lines().getFirst().vatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.lines().getFirst().grossAmount()),
                () -> Assertions.assertEquals(new BigDecimal("590.40"), response.totalGross())
        );
    }

    @Test
    void getInvoice_invoiceNotExists_throwsInvoiceNotFoundException() {
        //given
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());
        //when //then
        InvoiceNotFoundException ex = Assertions.assertThrows(
                InvoiceNotFoundException.class,
                () -> invoiceService.getInvoice(999L)
        );
        Assertions.assertEquals("Invoice with id = 999 doesn't exist", ex.getMessage());
    }

    @Test
    void getInvoiceByOrder_invoiceExists_returnsInvoiceResponse() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        when(invoiceRepository.findByOrderId(10L)).thenReturn(Optional.of(invoice));
        //when
        InvoiceResponse response = invoiceService.getInvoiceByOrder(10L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, response.orderId()),
                () -> Assertions.assertEquals("order-1", response.orderNumber())
        );
    }

    @Test
    void getInvoiceByOrder_invoiceNotExists_throwsInvoiceNotFoundException() {
        //given
        when(invoiceRepository.findByOrderId(999L)).thenReturn(Optional.empty());
        //when //then
        InvoiceNotFoundException ex = Assertions.assertThrows(
                InvoiceNotFoundException.class,
                () -> invoiceService.getInvoiceByOrder(999L)
        );
        Assertions.assertEquals("Invoice for order with id = 999 doesn't exist", ex.getMessage());
    }

    @Test
    void getInvoices_emailGiven_findByBuyerEmailCalled() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        ReflectionTestUtils.setField(invoice, "id", 1L);
        invoice.addLine("Headphones ABC", null, 1, new BigDecimal("240.00"), new BigDecimal("0.23"));
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        when(invoiceRepository.findByBuyerEmailIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice), PageRequest.of(0, 20), 1));
        //when
        PageResponse<InvoiceSummaryResponse> result = invoiceService.getInvoices("jan@example.com", new PaginationRequest(null, null, null, null));
        //then
        Mockito.verify(invoiceRepository).findByBuyerEmailIgnoreCase(emailCaptor.capture(), any(Pageable.class));
        Mockito.verify(invoiceRepository, Mockito.never()).findAll(any(Pageable.class));
        Assertions.assertAll(
                () -> Assertions.assertEquals("jan@example.com", emailCaptor.getValue()),
                () -> Assertions.assertEquals(1, result.content().size()),
                () -> Assertions.assertEquals("FV/2026/11/000001", result.content().getFirst().invoiceNumber()),
                () -> Assertions.assertEquals(new BigDecimal("295.20"), result.content().getFirst().totalGross())
        );
    }

    @Test
    void getInvoices_emailNull_findAllCalled() {
        //given
        when(invoiceRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        //when
        PageResponse<InvoiceSummaryResponse> result = invoiceService.getInvoices(null, new PaginationRequest(null, null, null, null));
        //then
        Mockito.verify(invoiceRepository).findAll(any(Pageable.class));
        Mockito.verify(invoiceRepository, Mockito.never()).findByBuyerEmailIgnoreCase(any(), any());
        Assertions.assertTrue(result.content().isEmpty());
    }
}
