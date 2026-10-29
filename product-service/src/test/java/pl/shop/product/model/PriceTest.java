package pl.shop.product.model;

import org.junit.jupiter.api.Test;
import pl.shop.product.exception.InvalidCurrencyException;
import pl.shop.product.exception.InvalidNetPriceException;
import pl.shop.product.exception.InvalidVatRateException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PriceTest {

    @Test
    void create_dataCorrect_vatAndGrossCalculated() {
        BigDecimal vatRate = new BigDecimal("0.23");
        Price price = new Price(new BigDecimal("100.00"), vatRate, "PLN");

        assertAll(
                () -> assertEquals(new BigDecimal("100.00"), price.getNetPrice()),
                () -> assertEquals(new BigDecimal("23.00"), price.getVatAmount()),
                () -> assertEquals(new BigDecimal("123.00"), price.getGrossPrice()),
                () -> assertEquals("PLN", price.getCurrency())
        );
    }
    @Test
    void create_vatWithMoreThanTwoDecimals_vatRoundedHalfUp() {
        BigDecimal vatRate = new BigDecimal("0.23");
        Price price = new Price(new BigDecimal("10.05"), vatRate, "PLN");

        assertAll(
                () -> assertEquals(new BigDecimal("2.31"), price.getVatAmount()),
                () -> assertEquals(new BigDecimal("12.36"), price.getGrossPrice())
        );
    }

    @Test
    void create_netWithThreeDecimals_netRoundedToTwoDecimals() {
        BigDecimal vatRate = new BigDecimal("0.23");
        Price price = new Price(new BigDecimal("99.999"), vatRate, "PLN");

        assertEquals(new BigDecimal("100.00"), price.getNetPrice());
    }

    @Test
    void create_zeroNetAndZeroVat_zeroGrossReturned() {
        Price price = new Price(BigDecimal.ZERO, BigDecimal.ZERO, "PLN");

        assertAll(
                () -> assertEquals(new BigDecimal("0.00"), price.getVatAmount()),
                () -> assertEquals(new BigDecimal("0.00"), price.getGrossPrice())
        );
    }

    @Test
    void create_netPriceNull_invalidNetPriceExceptionThrown() {
        BigDecimal vatRate = new BigDecimal("0.23");
        assertThrows(InvalidNetPriceException.class,
                () -> new Price(null, vatRate, "PLN"));
    }

    @Test
    void create_netPriceNegative_invalidNetPriceExceptionThrown() {
        BigDecimal vatRate = new BigDecimal("0.23");
        assertThrows(InvalidNetPriceException.class,
                () -> new Price(new BigDecimal("-0.01"), vatRate, "PLN"));
    }

    @Test
    void create_vatRateOutOfRange_invalidVatRateExceptionThrown() {
        BigDecimal vatRate = new BigDecimal("-0.01");
        assertThrows(InvalidVatRateException.class,
                () -> new Price(new BigDecimal("100.00"), vatRate, "PLN"));
    }

    @Test
    void create_currencyBlank_invalidCurrencyExceptionThrown() {
        BigDecimal vatRate = new BigDecimal("0.23");
        String currency = " ";
        assertThrows(InvalidCurrencyException.class,
                () -> new Price(new BigDecimal("100.00"), vatRate, currency));
    }

    @Test
    void withNetPrice_dataCorrect_newPriceReturnedOriginalUnchanged() {
        BigDecimal vatRate = new BigDecimal("0.23");
        Price original = new Price(new BigDecimal("100.00"), vatRate, "PLN");

        Price changed = original.withNetPrice(new BigDecimal("200.00"));

        assertAll(
                () -> assertNotSame(original, changed),
                () -> assertEquals(new BigDecimal("100.00"), original.getNetPrice()),
                () -> assertEquals(new BigDecimal("200.00"), changed.getNetPrice()),
                () -> assertEquals(new BigDecimal("246.00"), changed.getGrossPrice())
        );
    }

    @Test
    void withNetPrice_netPriceNegative_invalidNetPriceExceptionThrown() {
        BigDecimal vatRate = new BigDecimal("0.23");
        Price price = new Price(new BigDecimal("100.00"), vatRate, "PLN");

        assertThrows(InvalidNetPriceException.class,
                () -> price.withNetPrice(new BigDecimal("-1")));
    }
}

