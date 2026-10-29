package pl.shop.product.model;

import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.shop.product.exception.InvalidCurrencyException;
import pl.shop.product.exception.InvalidNetPriceException;
import pl.shop.product.exception.InvalidVatRateException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Price {
    private BigDecimal netPrice;
    private BigDecimal vatRate;
    private String currency;

    public Price(BigDecimal netPrice, BigDecimal vatRate, String currency) {
        validateNetPrice(netPrice);
        validateVatRate(vatRate);
        validateCurrency(currency);

        this.netPrice = netPrice.setScale(2, RoundingMode.HALF_UP);
        this.vatRate = vatRate.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public BigDecimal getVatAmount() {
        return netPrice.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getGrossPrice() {
        return netPrice.add(getVatAmount());
    }

    public Price withNetPrice(BigDecimal newNetPrice) {
        return new Price(newNetPrice, vatRate, currency);
    }

    public Price withVatRate(BigDecimal newVatRate) {
        return new Price(netPrice, newVatRate, currency);
    }

    private void validateNetPrice(BigDecimal netPrice) {
        if (netPrice == null || netPrice.signum() < 0) {
            throw new InvalidNetPriceException(netPrice);
        }
    }

    private void validateVatRate(BigDecimal vatRate) {
        if (vatRate == null || vatRate.signum() < 0 || vatRate.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidVatRateException(vatRate);
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidCurrencyException(currency);
        }
    }
}