package pl.shop.invoice.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.shop.invoice.exception.InvalidInvoiceLineException;
import pl.shop.invoice.exception.InvalidInvoiceStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceTest {

    @Test
    void issue_dataCorrect_invoiceIssuedWithoutLines() {
        //given
        Buyer buyer = new Buyer("Jan", "Kowalski", "jan@example.com");
        Address address = new Address("Długa 5/3", "Gdańsk", "80-001", "PL");
        LocalDateTime issuedAt = LocalDateTime.of(2026, 11, 2, 12, 0);
        //when
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", buyer, address, "PLN", issuedAt);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals("FV/2026/11/000001", invoice.getInvoiceNumber()),
                () -> Assertions.assertEquals(10L, invoice.getOrderId()),
                () -> Assertions.assertEquals("order-1", invoice.getOrderNumber()),
                () -> Assertions.assertEquals(InvoiceStatus.ISSUED, invoice.getStatus()),
                () -> Assertions.assertSame(buyer, invoice.getBuyer()),
                () -> Assertions.assertSame(address, invoice.getBillingAddress()),
                () -> Assertions.assertEquals("PLN", invoice.getCurrency()),
                () -> Assertions.assertEquals(issuedAt, invoice.getIssuedAt()),
                () -> Assertions.assertNull(invoice.getCancelledAt()),
                () -> Assertions.assertTrue(invoice.getLines().isEmpty()),
                () -> Assertions.assertFalse(invoice.isCancelled())
        );
    }

    @Test
    void addLine_dataCorrect_lineAddedWithCalculatedAmounts() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        //when
        InvoiceLine line = invoice.addLine("Gaming PC Pro", "Graphics card: RTX 4070", 2,
                new BigDecimal("5855.00"), new BigDecimal("0.23"));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, invoice.getLines().size()),
                () -> Assertions.assertSame(invoice, line.getInvoice()),
                () -> Assertions.assertEquals("Gaming PC Pro", line.getProductName()),
                () -> Assertions.assertEquals("Graphics card: RTX 4070", line.getDescription()),
                () -> Assertions.assertEquals(2, line.getQuantity()),
                () -> Assertions.assertEquals(new BigDecimal("11710.00"), line.getNetAmount()),
                () -> Assertions.assertEquals(new BigDecimal("2693.30"), line.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("14403.30"), line.getGrossAmount())
        );
    }

    @Test
    void addLine_quantityZero_throwsInvalidInvoiceLineException() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        //when //then
        InvalidInvoiceLineException ex = Assertions.assertThrows(
                InvalidInvoiceLineException.class,
                () -> invoice.addLine("Headphones ABC", null, 0, new BigDecimal("240.00"), new BigDecimal("0.23"))
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_INVOICE_LINE", ex.getErrorCode()),
                () -> Assertions.assertEquals("Quantity must be at least 1. Given: 0", ex.getMessage()),
                () -> Assertions.assertTrue(invoice.getLines().isEmpty())
        );
    }

    @Test
    void addLine_unitNetPriceNegative_throwsInvalidInvoiceLineException() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        //when //then
        InvalidInvoiceLineException ex = Assertions.assertThrows(
                InvalidInvoiceLineException.class,
                () -> invoice.addLine("Headphones ABC", null, 1, new BigDecimal("-1.00"), new BigDecimal("0.23"))
        );
        Assertions.assertEquals("Unit net price cannot be negative. Given: -1.00", ex.getMessage());
    }

    @Test
    void addLine_vatRateAboveOne_throwsInvalidInvoiceLineException() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        //when //then
        Assertions.assertThrows(
                InvalidInvoiceLineException.class,
                () -> invoice.addLine("Headphones ABC", null, 1, new BigDecimal("240.00"), new BigDecimal("1.50"))
        );
    }

    @Test
    void addLine_invoiceCancelled_throwsInvalidInvoiceStatusException() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        invoice.cancel(LocalDateTime.of(2026, 11, 3, 9, 0));
        //when //then
        InvalidInvoiceStatusException ex = Assertions.assertThrows(
                InvalidInvoiceStatusException.class,
                () -> invoice.addLine("Headphones ABC", null, 1, new BigDecimal("240.00"), new BigDecimal("0.23"))
        );
        Assertions.assertEquals("Invoice FV/2026/11/000001 cannot be modified in status CANCELLED", ex.getMessage());
    }

    @Test
    void getTotals_multipleLines_sumOfLinesReturned() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        invoice.addLine("Gaming PC Pro", null, 1, new BigDecimal("4800.00"), new BigDecimal("0.23"));
        invoice.addLine("Headphones ABC", null, 2, new BigDecimal("240.00"), new BigDecimal("0.23"));
        //when
        BigDecimal net = invoice.getTotalNet();
        BigDecimal vat = invoice.getTotalVat();
        BigDecimal gross = invoice.getTotalGross();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("5280.00"), net),
                () -> Assertions.assertEquals(new BigDecimal("1214.40"), vat),
                () -> Assertions.assertEquals(new BigDecimal("6494.40"), gross)
        );
    }

    @Test
    void cancel_invoiceIssued_statusCancelledAndDateSet() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 11, 3, 9, 0);
        //when
        invoice.cancel(cancelledAt);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus()),
                () -> Assertions.assertTrue(invoice.isCancelled()),
                () -> Assertions.assertEquals(cancelledAt, invoice.getCancelledAt())
        );
    }

    @Test
    void cancel_invoiceAlreadyCancelled_throwsInvalidInvoiceStatusException() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        LocalDateTime firstCancel = LocalDateTime.of(2026, 11, 3, 9, 0);
        invoice.cancel(firstCancel);
        //when //then
        InvalidInvoiceStatusException ex = Assertions.assertThrows(
                InvalidInvoiceStatusException.class,
                () -> invoice.cancel(LocalDateTime.of(2026, 11, 4, 9, 0))
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_INVOICE_STATUS", ex.getErrorCode()),
                () -> Assertions.assertEquals(firstCancel, invoice.getCancelledAt())
        );
    }

    @Test
    void getLines_listModified_throwsUnsupportedOperationException() {
        //given
        Invoice invoice = Invoice.issue("FV/2026/11/000001", 10L, "order-1", new Buyer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0));
        //when //then
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> invoice.getLines().clear()
        );
    }
}
