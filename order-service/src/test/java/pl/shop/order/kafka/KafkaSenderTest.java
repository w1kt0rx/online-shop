package pl.shop.order.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.order.kafka.event.OrderCancelledEvent;
import pl.shop.order.kafka.event.OrderPlacedEvent;
import pl.shop.order.mapper.OrderEventMapper;
import pl.shop.order.model.Address;
import pl.shop.order.model.Customer;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItemComponent;
import pl.shop.order.model.Price;

import java.math.BigDecimal;
import java.util.List;

public class KafkaSenderTest {
    private KafkaSender kafkaSender;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        this.kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        this.kafkaSender = new KafkaSender(kafkaTemplate, Mappers.getMapper(OrderEventMapper.class));
        ReflectionTestUtils.setField(kafkaSender, "orderPlacedTopic", "order-placed");
        ReflectionTestUtils.setField(kafkaSender, "orderCancelledTopic", "order-cancelled");
    }

    @Test
    void sendOrderPlaced_orderGiven_eventWithSnapshotSentToOrderPlacedTopic() {
        //given
        Order order = Order.place(7L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("6042.00"), new BigDecimal("0.23"), "PLN"), 1, List.of(
                new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070"),
                new OrderItemComponent(4L, 4L, "RAM", "RAM 32 GB DDR5")
        ));
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when
        kafkaSender.sendOrderPlaced(order);
        //then
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        OrderPlacedEvent event = (OrderPlacedEvent) eventCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals("order-placed", topicCaptor.getValue()),
                () -> Assertions.assertEquals(order.getOrderNumber(), keyCaptor.getValue()),
                () -> Assertions.assertEquals(10L, event.orderId()),
                () -> Assertions.assertEquals(order.getOrderNumber(), event.orderNumber()),
                () -> Assertions.assertEquals(7L, event.cartId()),
                () -> Assertions.assertEquals(order.getCreatedAt(), event.placedAt()),
                () -> Assertions.assertEquals("jan@example.com", event.customer().email()),
                () -> Assertions.assertEquals("Gdańsk", event.shippingAddress().city()),
                () -> Assertions.assertEquals(1, event.items().size()),
                () -> Assertions.assertEquals(new BigDecimal("6042.00"), event.items().getFirst().unitNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), event.items().getFirst().vatRate()),
                () -> Assertions.assertEquals(List.of("Graphics card: RTX 4070", "RAM: RAM 32 GB DDR5"), event.items().getFirst().components()),
                () -> Assertions.assertEquals(new BigDecimal("6042.00"), event.totalNet()),
                () -> Assertions.assertEquals(new BigDecimal("1389.66"), event.totalVat()),
                () -> Assertions.assertEquals(new BigDecimal("7431.66"), event.totalGross()),
                () -> Assertions.assertEquals("PLN", event.currency())
        );
    }

    @Test
    void sendOrderCancelled_orderGiven_eventSentToOrderCancelledTopic() {
        //given
        Order order = Order.place(7L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        ReflectionTestUtils.setField(order, "id", 10L);
        order.cancel();
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when
        kafkaSender.sendOrderCancelled(order);
        //then
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("order-cancelled", topicCaptor.getValue()),
                () -> Assertions.assertEquals(order.getOrderNumber(), keyCaptor.getValue()),
                () -> Assertions.assertEquals(new OrderCancelledEvent(10L, order.getOrderNumber(), order.getCancelledAt()), eventCaptor.getValue())
        );
    }
}
