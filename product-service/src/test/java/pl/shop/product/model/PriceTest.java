package pl.shop.product.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.shop.product.exception.InvalidCurrencyException;
import pl.shop.product.exception.InvalidNetPriceException;
import pl.shop.product.exception.InvalidVatRateException;

import java.math.BigDecimal;

public class PriceTest {

    @Test
    void create_dataCorrect_vatAndGrossCalculated() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when
        Price price = new Price(netPrice, vatRate, "PLN");
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("100.00"), price.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), price.getVatRate()),
                () -> Assertions.assertEquals(new BigDecimal("23.00"), price.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("123.00"), price.getGrossPrice()),
                () -> Assertions.assertEquals("PLN", price.getCurrency())
        );
    }

    @Test
    void create_vatWithMoreThanTwoDecimals_vatRoundedHalfUp() {
        //given
        BigDecimal netPrice = new BigDecimal("10.05");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when
        Price price = new Price(netPrice, vatRate, "PLN");
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("2.31"), price.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("12.36"), price.getGrossPrice())
        );
    }

    @Test
    void create_netWithThreeDecimals_netRoundedToTwoDecimals() {
        //given
        BigDecimal netPrice = new BigDecimal("99.999");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when
        Price price = new Price(netPrice, vatRate, "PLN");
        //then
        Assertions.assertEquals(new BigDecimal("100.00"), price.getNetPrice());
    }

    @Test
    void create_zeroNetAndZeroVat_zeroGrossReturned() {
        //given
        BigDecimal netPrice = BigDecimal.ZERO;
        BigDecimal vatRate = BigDecimal.ZERO;
        //when
        Price price = new Price(netPrice, vatRate, "PLN");
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("0.00"), price.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("0.00"), price.getGrossPrice())
        );
    }

    @Test
    void create_netPriceNull_invalidNetPriceExceptionThrown() {
        //given
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        InvalidNetPriceException ex = Assertions.assertThrows(
                InvalidNetPriceException.class,
                () -> new Price(null, vatRate, "PLN")
        );
        Assertions.assertEquals("INVALID_NET_PRICE", ex.getErrorCode());
    }

    @Test
    void create_netPriceNegative_invalidNetPriceExceptionThrown() {
        //given
        BigDecimal netPrice = new BigDecimal("-0.01");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        InvalidNetPriceException ex = Assertions.assertThrows(
                InvalidNetPriceException.class,
                () -> new Price(netPrice, vatRate, "PLN")
        );
        Assertions.assertEquals("NetPrice of product cannot be negative. Given netPrice: -0.01", ex.getMessage());
    }

    @Test
    void create_vatRateNegative_invalidVatRateExceptionThrown() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("-0.01");
        //when //then
        InvalidVatRateException ex = Assertions.assertThrows(
                InvalidVatRateException.class,
                () -> new Price(netPrice, vatRate, "PLN")
        );
        Assertions.assertEquals("INVALID_VAT_RATE", ex.getErrorCode());
    }

    @Test
    void create_vatRateAboveOne_invalidVatRateExceptionThrown() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("1.01");
        //when //then
        InvalidVatRateException ex = Assertions.assertThrows(
                InvalidVatRateException.class,
                () -> new Price(netPrice, vatRate, "PLN")
        );
        Assertions.assertEquals("Vat rate has to be in range 0 - 1. Given rate equal to: 1.01", ex.getMessage());
    }

    @Test
    void create_vatRateNull_invalidVatRateExceptionThrown() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        //when //then
        Assertions.assertThrows(
                InvalidVatRateException.class,
                () -> new Price(netPrice, null, "PLN")
        );
    }

    @Test
    void create_currencyBlank_invalidCurrencyExceptionThrown() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        InvalidCurrencyException ex = Assertions.assertThrows(
                InvalidCurrencyException.class,
                () -> new Price(netPrice, vatRate, " ")
        );
        Assertions.assertEquals("INVALID_CURRENCY", ex.getErrorCode());
    }

    @Test
    void create_currencyNull_invalidCurrencyExceptionThrown() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        Assertions.assertThrows(
                InvalidCurrencyException.class,
                () -> new Price(netPrice, vatRate, null)
        );
    }

    @Test
    void withNetPrice_dataCorrect_newPriceReturnedOriginalUnchanged() {
        //given
        Price original = new Price(new BigDecimal("100.00"), new BigDecimal("0.23"), "PLN");
        //when
        Price changed = original.withNetPrice(new BigDecimal("200.00"));
        //then
        Assertions.assertAll(
                () -> Assertions.assertNotSame(original, changed),
                () -> Assertions.assertEquals(new BigDecimal("100.00"), original.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("200.00"), changed.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), changed.getVatRate()),
                () -> Assertions.assertEquals("PLN", changed.getCurrency()),
                () -> Assertions.assertEquals(new BigDecimal("246.00"), changed.getGrossPrice())
        );
    }

    @Test
    void withNetPrice_netPriceNegative_invalidNetPriceExceptionThrown() {
        //given
        Price price = new Price(new BigDecimal("100.00"), new BigDecimal("0.23"), "PLN");
        //when //then
        Assertions.assertThrows(
                InvalidNetPriceException.class,
                () -> price.withNetPrice(new BigDecimal("-1"))
        );
    }
}
