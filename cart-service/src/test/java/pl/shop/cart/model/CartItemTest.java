package pl.shop.cart.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.shop.cart.exception.InvalidQuantityException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class CartItemTest {

    @Test
    void create_quantityZero_throwsInvalidQuantityException() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        //when //then
        InvalidQuantityException ex = Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> new CartItem(cart, 5L, "Headphones ABC", price, 0, List.of())
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_QUANTITY", ex.getErrorCode()),
                () -> Assertions.assertEquals("Quantity must be between 1 and 99. Given: 0", ex.getMessage())
        );
    }

    @Test
    void create_quantityAboveMax_throwsInvalidQuantityException() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        //when //then
        Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> new CartItem(cart, 5L, "Headphones ABC", price, 100, List.of())
        );
    }

    @Test
    void getTotalPrice_quantityThree_unitPriceMultiplied() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 5L, "Headphones ABC", price, 3, List.of());
        //when
        Price total = item.getTotalPrice();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("720.00"), total.getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("165.60"), total.getVatAmount()),
                () -> Assertions.assertEquals(new BigDecimal("885.60"), total.getGrossPrice())
        );
    }

    @Test
    void getStockProductIds_itemWithComponents_productAndComponentIdsReturned() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 1L, "Gaming PC Pro", price, 1, List.of(
                new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070"),
                new CartItemComponent(12L, 7L, "RAM", "RAM 16 GB DDR5")
        ));
        //when
        List<Long> stockProductIds = item.getStockProductIds();
        //then
        Assertions.assertEquals(List.of(1L, 4L, 7L), stockProductIds);
    }

    @Test
    void getConfigurationIds_itemWithComponents_configurationIdsReturned() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 1L, "Gaming PC Pro", price, 1, List.of(
                new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070"),
                new CartItemComponent(12L, 7L, "RAM", "RAM 16 GB DDR5")
        ));
        //when
        Set<Long> configurationIds = item.getConfigurationIds();
        //then
        Assertions.assertEquals(Set.of(11L, 12L), configurationIds);
    }

    @Test
    void isSameProductAs_sameProductAndConfigurationsInDifferentOrder_returnsTrue() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 1L, "Gaming PC Pro", price, 1, List.of(
                new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070"),
                new CartItemComponent(12L, 7L, "RAM", "RAM 16 GB DDR5")
        ));
        List<CartItemComponent> other = List.of(
                new CartItemComponent(12L, 7L, "RAM", "RAM 16 GB DDR5"),
                new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")
        );
        //when
        boolean same = item.isSameProductAs(1L, other);
        //then
        Assertions.assertTrue(same);
    }

    @Test
    void isSameProductAs_differentConfiguration_returnsFalse() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 1L, "Gaming PC Pro", price, 1, List.of(
                new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")
        ));
        List<CartItemComponent> other = List.of(new CartItemComponent(10L, 6L, "Graphics card", "RTX 4060"));
        //when
        boolean same = item.isSameProductAs(1L, other);
        //then
        Assertions.assertFalse(same);
    }

    @Test
    void isSameProductAs_differentProduct_returnsFalse() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 5L, "Headphones ABC", price, 1, List.of());
        //when
        boolean same = item.isSameProductAs(6L, List.of());
        //then
        Assertions.assertFalse(same);
    }

    @Test
    void increaseQuantity_resultAboveMax_throwsInvalidQuantityExceptionAndQuantityUnchanged() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 5L, "Headphones ABC", price, 98, List.of());
        //when //then
        InvalidQuantityException ex = Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> item.increaseQuantity(2)
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("Quantity must be between 1 and 99. Given: 100", ex.getMessage()),
                () -> Assertions.assertEquals(98, item.getQuantity())
        );
    }

    @Test
    void getComponents_listModified_throwsUnsupportedOperationException() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = new CartItem(cart, 5L, "Headphones ABC", price, 1, List.of());
        //when //then
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> item.getComponents().clear()
        );
    }
}
