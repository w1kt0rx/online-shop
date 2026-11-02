package pl.shop.cart.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.cart.exception.CartAlreadyOrderedException;
import pl.shop.cart.exception.CartItemNotFoundException;
import pl.shop.cart.exception.InvalidQuantityException;

import java.math.BigDecimal;
import java.util.List;

public class CartTest {

    @Test
    void create_noArguments_activeEmptyCartCreated() {
        //given
        //when
        Cart cart = Cart.create();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(CartStatus.ACTIVE, cart.getStatus()),
                () -> Assertions.assertNotNull(cart.getCreatedAt()),
                () -> Assertions.assertTrue(cart.getItems().isEmpty()),
                () -> Assertions.assertFalse(cart.isOrdered()),
                () -> Assertions.assertEquals(BigDecimal.ZERO, cart.getTotalNet()),
                () -> Assertions.assertEquals(BigDecimal.ZERO, cart.getTotalGross())
        );
    }

    @Test
    void addItem_newProduct_itemCreated() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        List<CartItemComponent> components = List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070"));
        //when
        CartItem item = cart.addItem(1L, "Gaming PC Pro", price, 2, components);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, cart.getItems().size()),
                () -> Assertions.assertSame(item, cart.getItems().getFirst()),
                () -> Assertions.assertSame(cart, item.getCart()),
                () -> Assertions.assertEquals(1L, item.getProductId()),
                () -> Assertions.assertEquals("Gaming PC Pro", item.getProductName()),
                () -> Assertions.assertEquals(2, item.getQuantity()),
                () -> Assertions.assertEquals(new BigDecimal("5855.00"), item.getUnitPrice().getNetPrice()),
                () -> Assertions.assertEquals(1, item.getComponents().size()),
                () -> Assertions.assertEquals(11L, item.getComponents().getFirst().getConfigurationId())
        );
    }

    @Test
    void addItem_sameProductAndConfiguration_quantityMerged() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        CartItem existing = cart.addItem(1L, "Gaming PC Pro", price, 1,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        //when
        CartItem item = cart.addItem(1L, "Gaming PC Pro", price, 2,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        //then
        Assertions.assertAll(
                () -> Assertions.assertSame(existing, item),
                () -> Assertions.assertEquals(1, cart.getItems().size()),
                () -> Assertions.assertEquals(3, item.getQuantity())
        );
    }

    @Test
    void addItem_sameProductDifferentConfiguration_separateItemCreated() {
        //given
        Cart cart = Cart.create();
        Price upgraded = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        Price base = new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN");
        cart.addItem(1L, "Gaming PC Pro", upgraded, 1,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        //when
        cart.addItem(1L, "Gaming PC Pro", base, 1,
                List.of(new CartItemComponent(10L, 6L, "Graphics card", "RTX 4060")));
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, cart.getItems().size()),
                () -> Assertions.assertEquals(new BigDecimal("5855.00"), cart.getItems().get(0).getUnitPrice().getNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("4800.00"), cart.getItems().get(1).getUnitPrice().getNetPrice())
        );
    }

    @Test
    void addItem_cartOrdered_throwsCartAlreadyOrderedException() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        ReflectionTestUtils.setField(cart, "status", CartStatus.ORDERED);
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        //when //then
        CartAlreadyOrderedException ex = Assertions.assertThrows(
                CartAlreadyOrderedException.class,
                () -> cart.addItem(5L, "Headphones ABC", price, 1, List.of())
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("CART_ALREADY_ORDERED", ex.getErrorCode()),
                () -> Assertions.assertEquals("Cart with id = 1 is already ordered", ex.getMessage()),
                () -> Assertions.assertTrue(cart.getItems().isEmpty())
        );
    }

    @Test
    void changeItemQuantity_itemExists_quantityChanged() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = cart.addItem(5L, "Headphones ABC", price, 1, List.of());
        ReflectionTestUtils.setField(item, "id", 100L);
        //when
        cart.changeItemQuantity(100L, 4);
        //then
        Assertions.assertEquals(4, item.getQuantity());
    }

    @Test
    void changeItemQuantity_quantityInvalid_throwsInvalidQuantityException() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = cart.addItem(5L, "Headphones ABC", price, 1, List.of());
        ReflectionTestUtils.setField(item, "id", 100L);
        //when //then
        Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> cart.changeItemQuantity(100L, 0)
        );
        Assertions.assertEquals(1, item.getQuantity());
    }

    @Test
    void changeItemQuantity_itemNotExists_throwsCartItemNotFoundException() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        //when //then
        CartItemNotFoundException ex = Assertions.assertThrows(
                CartItemNotFoundException.class,
                () -> cart.changeItemQuantity(999L, 2)
        );
        Assertions.assertEquals("Item with id = 999 not found in cart with id = 1", ex.getMessage());
    }

    @Test
    void removeItem_itemExists_itemRemoved() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = cart.addItem(5L, "Headphones ABC", price, 1, List.of());
        ReflectionTestUtils.setField(item, "id", 100L);
        //when
        cart.removeItem(100L);
        //then
        Assertions.assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void removeItem_itemNotExists_throwsCartItemNotFoundException() {
        //given
        Cart cart = Cart.create();
        ReflectionTestUtils.setField(cart, "id", 1L);
        //when //then
        CartItemNotFoundException ex = Assertions.assertThrows(
                CartItemNotFoundException.class,
                () -> cart.removeItem(999L)
        );
        Assertions.assertEquals("CART_ITEM_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void removeItem_cartOrdered_throwsCartAlreadyOrderedException() {
        //given
        Cart cart = Cart.create();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        CartItem item = cart.addItem(5L, "Headphones ABC", price, 1, List.of());
        ReflectionTestUtils.setField(item, "id", 100L);
        ReflectionTestUtils.setField(cart, "status", CartStatus.ORDERED);
        //when //then
        Assertions.assertThrows(
                CartAlreadyOrderedException.class,
                () -> cart.removeItem(100L)
        );
        Assertions.assertEquals(1, cart.getItems().size());
    }

    @Test
    void getTotals_multipleItems_sumOfItemTotalsReturned() {
        //given
        Cart cart = Cart.create();
        cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        cart.addItem(5L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 2, List.of());
        //when
        BigDecimal net = cart.getTotalNet();
        BigDecimal vat = cart.getTotalVat();
        BigDecimal gross = cart.getTotalGross();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("5280.00"), net),
                () -> Assertions.assertEquals(new BigDecimal("1214.40"), vat),
                () -> Assertions.assertEquals(new BigDecimal("6494.40"), gross)
        );
    }

    @Test
    void getItems_listModified_throwsUnsupportedOperationException() {
        //given
        Cart cart = Cart.create();
        //when //then
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> cart.getItems().clear()
        );
    }
}
