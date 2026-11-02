package pl.shop.order.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.order.exception.InvalidOrderStatusException;
import pl.shop.order.exception.InvalidQuantityException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderTest {

    @Test
    void place_dataCorrect_orderPlacedWithNumberAndStatus() {
        //given
        Customer customer = new Customer("Jan", "Kowalski", "jan@example.com");
        Address address = new Address("Długa 5/3", "Gdańsk", "80-001", "PL");
        //when
        Order order = Order.place(1L, customer, address);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, order.getCartId()),
                () -> Assertions.assertEquals(OrderStatus.PLACED, order.getStatus()),
                () -> Assertions.assertEquals(36, order.getOrderNumber().length()),
                () -> Assertions.assertNotNull(order.getCreatedAt()),
                () -> Assertions.assertNull(order.getCancelledAt()),
                () -> Assertions.assertSame(customer, order.getCustomer()),
                () -> Assertions.assertSame(address, order.getShippingAddress()),
                () -> Assertions.assertTrue(order.getItems().isEmpty()),
                () -> Assertions.assertFalse(order.isCancelled())
        );
    }

    @Test
    void place_twoOrders_differentOrderNumbers() {
        //given
        Customer customer = new Customer("Jan", "Kowalski", "jan@example.com");
        Address address = new Address("Długa 5/3", "Gdańsk", "80-001", "PL");
        //when
        Order first = Order.place(1L, customer, address);
        Order second = Order.place(2L, customer, address);
        //then
        Assertions.assertNotEquals(first.getOrderNumber(), second.getOrderNumber());
    }

    @Test
    void addItem_dataCorrect_itemAdded() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        Price price = new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN");
        List<OrderItemComponent> components = List.of(new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070"));
        //when
        OrderItem item = order.addItem(5L, "Gaming PC Pro", price, 2, components);
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, order.getItems().size()),
                () -> Assertions.assertSame(item, order.getItems().getFirst()),
                () -> Assertions.assertSame(order, item.getOrder()),
                () -> Assertions.assertEquals(5L, item.getProductId()),
                () -> Assertions.assertEquals("Gaming PC Pro", item.getProductName()),
                () -> Assertions.assertEquals(2, item.getQuantity()),
                () -> Assertions.assertEquals(new BigDecimal("11710.00"), item.getTotalPrice().getNetPrice()),
                () -> Assertions.assertEquals(List.of(5L, 2L), item.getStockProductIds()),
                () -> Assertions.assertEquals(1, order.getItemCount())
        );
    }

    @Test
    void addItem_quantityZero_throwsInvalidQuantityException() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        //when //then
        InvalidQuantityException ex = Assertions.assertThrows(
                InvalidQuantityException.class,
                () -> order.addItem(6L, "Headphones ABC", price, 0, List.of())
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_QUANTITY", ex.getErrorCode()),
                () -> Assertions.assertEquals("Quantity must be at least 1. Given: 0", ex.getMessage()),
                () -> Assertions.assertTrue(order.getItems().isEmpty())
        );
    }

    @Test
    void addItem_orderCancelled_throwsInvalidOrderStatusException() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.cancel();
        Price price = new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN");
        //when //then
        InvalidOrderStatusException ex = Assertions.assertThrows(
                InvalidOrderStatusException.class,
                () -> order.addItem(6L, "Headphones ABC", price, 1, List.of())
        );
        Assertions.assertEquals("Order with id = 10 cannot be modified in status CANCELLED", ex.getMessage());
    }

    @Test
    void stockRequirements_itemsShareComponent_quantitiesSummed() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        order.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("6042.00"), new BigDecimal("0.23"), "PLN"), 2, List.of(
                new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070"),
                new OrderItemComponent(4L, 4L, "RAM", "RAM 32 GB DDR5")
        ));
        order.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        //when
        Map<Long, Integer> requirements = order.stockRequirements();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(3, requirements.size()),
                () -> Assertions.assertEquals(2, requirements.get(5L)),
                () -> Assertions.assertEquals(3, requirements.get(2L)),
                () -> Assertions.assertEquals(2, requirements.get(4L)),
                () -> Assertions.assertEquals(List.of(5L, 2L, 4L), List.copyOf(requirements.keySet()))
        );
    }

    @Test
    void cancel_orderPlaced_statusCancelledAndDateSet() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        //when
        order.cancel();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus()),
                () -> Assertions.assertTrue(order.isCancelled()),
                () -> Assertions.assertNotNull(order.getCancelledAt())
        );
    }

    @Test
    void cancel_orderAlreadyCancelled_throwsInvalidOrderStatusException() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.cancel();
        //when //then
        InvalidOrderStatusException ex = Assertions.assertThrows(
                InvalidOrderStatusException.class,
                order::cancel
        );
        Assertions.assertAll(
                () -> Assertions.assertEquals("INVALID_ORDER_STATUS", ex.getErrorCode()),
                () -> Assertions.assertEquals("Order with id = 10 cannot be modified in status CANCELLED", ex.getMessage())
        );
    }

    @Test
    void getTotals_multipleItems_sumOfItemTotalsReturned() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        order.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("4800.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        order.addItem(6L, "Headphones ABC", new Price(new BigDecimal("240.00"), new BigDecimal("0.23"), "PLN"), 2, List.of());
        //when
        BigDecimal net = order.getTotalNet();
        BigDecimal vat = order.getTotalVat();
        BigDecimal gross = order.getTotalGross();
        //then
        Assertions.assertAll(
                () -> Assertions.assertEquals(new BigDecimal("5280.00"), net),
                () -> Assertions.assertEquals(new BigDecimal("1214.40"), vat),
                () -> Assertions.assertEquals(new BigDecimal("6494.40"), gross),
                () -> Assertions.assertEquals(2, order.getItemCount())
        );
    }

    @Test
    void getItems_listModified_throwsUnsupportedOperationException() {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        //when //then
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> order.getItems().clear()
        );
    }
}
