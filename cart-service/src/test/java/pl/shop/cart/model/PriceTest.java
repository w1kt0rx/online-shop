package pl.shop.cart.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.shop.cart.exception.InvalidCurrencyException;
import pl.shop.cart.exception.InvalidNetPriceException;
import pl.shop.cart.exception.InvalidVatRateException;

import java.math.BigDecimal;

public class PriceTest {

    @Test
    void create_dataCorrect_vatAndGrossCalculated() {
        //given
        BigDecimal netPrice = new BigDecimal("4800.00");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when
        Price price = new Price(netPrice, vatRate, "PLN");
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), price.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), price.getVatRate()),
                () -> Assertions.assertEquals(new BigDecimal("1104.00"), price.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("5904.00"), price.getGrossPrice()),
                () -> Assertions.assertEquals("PLN", price.getCurrency())
        );
    }

    @Test
    void create_netWithThreeDecimals_netRoundedHalfUp() {
        //given
        BigDecimal netPrice = new BigDecimal("10.005");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when
        Price price = new Price(netPrice, vatRate, "PLN");
        //then
        Assertions.assertEquals(new BigDecimal("10.01"), price.getNetPrice());
    }

    @Test
    void create_netPriceNegative_throwsInvalidNetPriceException() {
        //given
        BigDecimal netPrice = new BigDecimal("-0.01");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        InvalidNetPriceException ex = Assertions.assertThrows(
                InvalidNetPriceException.class,
                () -> new Price(netPrice, vatRate, "PLN")
        );
        Assertions.assertEquals("INVALID_NET_PRICE", ex.getErrorCode());
    }

    @Test
    void create_netPriceNull_throwsInvalidNetPriceException() {
        //given
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        Assertions.assertThrows(
                InvalidNetPriceException.class,
                () -> new Price(null, vatRate, "PLN")
        );
    }

    @Test
    void create_vatRateAboveOne_throwsInvalidVatRateException() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("1.50");
        //when //then
        InvalidVatRateException ex = Assertions.assertThrows(
                InvalidVatRateException.class,
                () -> new Price(netPrice, vatRate, "PLN")
        );
        Assertions.assertEquals("INVALID_VAT_RATE", ex.getErrorCode());
    }

    @Test
    void create_currencyBlank_throwsInvalidCurrencyException() {
        //given
        BigDecimal netPrice = new BigDecimal("100.00");
        BigDecimal vatRate = new BigDecimal("0.23");
        //when //then
        InvalidCurrencyException ex = Assertions.assertThrows(
                InvalidCurrencyException.class,
                () -> new Price(netPrice, vatRate, "")
        );
        Assertions.assertEquals("INVALID_CURRENCY", ex.getErrorCode());
    }

    @Test
    void multiply_quantityGiven_netMultipliedAndVatRecalculated() {
        //given
        Price price = new Price(new BigDecimal("10.05"), new BigDecimal("0.23"), "PLN");
        //when
        Price total = price.multiply(3);
        //then
        Assertions.assertAll(
                () -> Assertions.assertNotSame(price, total),
                () -> Assertions.assertEquals(new BigDecimal("10.05"), price.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("30.15"), total.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("6.93"), total.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("37.08"), total.getGrossPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), total.getVatRate()),
                () -> Assertions.assertEquals("PLN", total.getCurrency())
        );
    }
}
