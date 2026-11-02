package pl.shop.product.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import pl.shop.product.kafka.event.ProductCreatedEvent;
import pl.shop.product.kafka.event.ProductDeletedEvent;
import pl.shop.product.model.Price;
import pl.shop.product.model.Product;

import java.math.BigDecimal;

public class KafkaSenderTest {
    private KafkaSender kafkaSender;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        this.kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        this.kafkaSender = new KafkaSender(kafkaTemplate);
        ReflectionTestUtils.setField(kafkaSender, "productCreatedTopic", "product-created");
        ReflectionTestUtils.setField(kafkaSender, "productDeletedTopic", "product-deleted");
    }

    @Test
    void sendProductCreated_productGiven_eventSentToProductCreatedTopic() {
        //given
        Product product = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(product, "id", 4L);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when
        kafkaSender.sendProductCreated(product);
        //then
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("product-created", topicCaptor.getValue()),
                () -> Assertions.assertEquals("4", keyCaptor.getValue()),
                () -> Assertions.assertEquals(new ProductCreatedEvent(4L, "RTX 4070"), eventCaptor.getValue())
        );
    }

    @Test
    void sendProductDeleted_productGiven_eventSentToProductDeletedTopic() {
        //given
        Product product = new Product("RTX 4070", "Electronics", "NVIDIA",
                new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), true);
        ReflectionTestUtils.setField(product, "id", 4L);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when
        kafkaSender.sendProductDeleted(product);
        //then
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("product-deleted", topicCaptor.getValue()),
                () -> Assertions.assertEquals("4", keyCaptor.getValue()),
                () -> Assertions.assertEquals(new ProductDeletedEvent(4L, "RTX 4070"), eventCaptor.getValue())
        );
    }
}
