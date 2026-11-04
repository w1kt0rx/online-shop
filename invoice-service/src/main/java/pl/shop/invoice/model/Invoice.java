package pl.shop.invoice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.invoice.exception.InvalidInvoiceStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 20)
    private String invoiceNumber;

    @Column(nullable = false, unique = true, updatable = false)
    private Long orderId;

    @Column(nullable = false, updatable = false, length = 36)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Embedded
    private Buyer buyer;

    @Embedded
    private Address billingAddress;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<InvoiceLine> lines = new ArrayList<>();

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime cancelledAt;

    @Version
    private Long version;

    public static Invoice issue(String invoiceNumber, Long orderId, String orderNumber, Buyer buyer,
                                Address billingAddress, String currency, LocalDateTime issuedAt) {
        Invoice invoice = new Invoice();
        invoice.invoiceNumber = invoiceNumber;
        invoice.orderId = orderId;
        invoice.orderNumber = orderNumber;
        invoice.buyer = buyer;
        invoice.billingAddress = billingAddress;
        invoice.currency = currency;
        invoice.issuedAt = issuedAt;
        invoice.status = InvoiceStatus.ISSUED;
        return invoice;
    }

    public InvoiceLine addLine(String productName, String description, int quantity,
                               BigDecimal unitNetPrice, BigDecimal vatRate) {
        validateIssued();
        InvoiceLine line = new InvoiceLine(this, productName, description, quantity, unitNetPrice, vatRate);
        lines.add(line);
        return line;
    }

    public void cancel(LocalDateTime cancelledAt) {
        validateIssued();
        this.status = InvoiceStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public boolean isCancelled() {
        return status == InvoiceStatus.CANCELLED;
    }

    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalNet() {
        return lines.stream()
                .map(InvoiceLine::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalVat() {
        return lines.stream()
                .map(InvoiceLine::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGross() {
        return getTotalNet().add(getTotalVat());
    }

    private void validateIssued() {
        if (isCancelled()) {
            throw new InvalidInvoiceStatusException(invoiceNumber, status);
        }
    }
}
