package pl.shop.invoice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.invoice.exception.InvalidInvoiceLineException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "invoice_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false)
    private String productName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal unitNetPrice;

    @Column(nullable = false)
    private BigDecimal vatRate;

    InvoiceLine(Invoice invoice, String productName, String description, int quantity,
                BigDecimal unitNetPrice, BigDecimal vatRate) {
        validateQuantity(quantity);
        validateUnitNetPrice(unitNetPrice);
        validateVatRate(vatRate);
        this.invoice = invoice;
        this.productName = productName;
        this.description = description;
        this.quantity = quantity;
        this.unitNetPrice = unitNetPrice.setScale(2, RoundingMode.HALF_UP);
        this.vatRate = vatRate.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getNetAmount() {
        return unitNetPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getVatAmount() {
        return getNetAmount().multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getGrossAmount() {
        return getNetAmount().add(getVatAmount());
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidInvoiceLineException("Quantity must be at least 1. Given: " + quantity);
        }
    }

    private void validateUnitNetPrice(BigDecimal unitNetPrice) {
        if (unitNetPrice == null || unitNetPrice.signum() < 0) {
            throw new InvalidInvoiceLineException("Unit net price cannot be negative. Given: " + unitNetPrice);
        }
    }

    private void validateVatRate(BigDecimal vatRate) {
        if (vatRate == null || vatRate.signum() < 0 || vatRate.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidInvoiceLineException("Vat rate has to be in range 0 - 1. Given: " + vatRate);
        }
    }
}
