package pl.shop.order.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import pl.shop.order.kafka.event.OrderCancelledEvent;
import pl.shop.order.model.Address;
import pl.shop.order.model.Customer;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItemComponent;
import pl.shop.order.model.OrderStatus;
import pl.shop.order.model.Price;
import pl.shop.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class CancelOrderIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderRepository orderRepository;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        orderRepository.deleteAll();
    }

    @Test
    void cancelOrder_orderPlaced_orderCancelledStockReleasedAndEventSent() throws Exception {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        order.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2, List.of(
                new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070"),
                new OrderItemComponent(4L, 4L, "RAM", "RAM 32 GB DDR5")
        ));
        order.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Order saved = orderRepository.save(order);
        stubFor(WireMock.post(urlEqualTo("/inventory/releases"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 5, "quantity": 10, "available": true},
                                  {"productId": 2, "quantity": 10, "available": true},
                                  {"productId": 4, "quantity": 20, "available": true}
                                ]
                                """)));
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when
        mockMvc.perform(post("/orders/{orderId}/cancel", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.orderNumber").value(saved.getOrderNumber()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.total.grossPrice").value(17201.55));
        //then
        Order cancelled = orderRepository.findById(saved.getId()).orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals(OrderStatus.CANCELLED, cancelled.getStatus()),
                () -> Assertions.assertNotNull(cancelled.getCancelledAt()),
                () -> Assertions.assertEquals(saved.getOrderNumber(), cancelled.getOrderNumber())
        );
        verify(1, postRequestedFor(urlEqualTo("/inventory/releases"))
                .withRequestBody(equalToJson("""
                        {
                          "orderNumber": "%s",
                          "items": [
                            {"productId": 5, "quantity": 2},
                            {"productId": 2, "quantity": 3},
                            {"productId": 4, "quantity": 2}
                          ]
                        }
                        """.formatted(saved.getOrderNumber()), true, false)));
        verify(0, postRequestedFor(urlEqualTo("/inventory/reservations")));
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        OrderCancelledEvent event = (OrderCancelledEvent) eventCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals("order-cancelled", topicCaptor.getValue()),
                () -> Assertions.assertEquals(saved.getOrderNumber(), keyCaptor.getValue()),
                () -> Assertions.assertEquals(saved.getId(), event.orderId()),
                () -> Assertions.assertEquals(saved.getOrderNumber(), event.orderNumber()),
                () -> Assertions.assertNotNull(event.cancelledAt())
        );
    }

    @Test
    void cancelOrder_orderCancelledTwice_conflictReturnedAndStockReleasedOnlyOnce() throws Exception {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        order.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Order saved = orderRepository.save(order);
        stubFor(WireMock.post(urlEqualTo("/inventory/releases"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"productId": 2, "quantity": 10, "available": true}]
                                """)));
        mockMvc.perform(post("/orders/{orderId}/cancel", saved.getId()))
                .andExpect(status().isOk());
        //when //then
        mockMvc.perform(post("/orders/{orderId}/cancel", saved.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATUS"))
                .andExpect(jsonPath("$.message").value("Order with id = " + saved.getId() + " cannot be modified in status CANCELLED"));
        Assertions.assertEquals(OrderStatus.CANCELLED, orderRepository.findById(saved.getId()).orElseThrow().getStatus());
        verify(1, postRequestedFor(urlEqualTo("/inventory/releases")));
        Mockito.verify(kafkaTemplate, Mockito.times(1)).send(any(), any(), any());
    }

    @Test
    void cancelOrder_inventoryReleaseFails_serviceUnavailableReturnedAndOrderStaysPlaced() throws Exception {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        order.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Order saved = orderRepository.save(order);
        stubFor(WireMock.post(urlEqualTo("/inventory/releases"))
                .willReturn(aResponse().withStatus(500)));
        //when //then
        mockMvc.perform(post("/orders/{orderId}/cancel", saved.getId()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("REMOTE_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("Remote service failed, try again later"));
        Order notCancelled = orderRepository.findById(saved.getId()).orElseThrow();
        Assertions.assertAll(
                () -> Assertions.assertEquals(OrderStatus.PLACED, notCancelled.getStatus()),
                () -> Assertions.assertNull(notCancelled.getCancelledAt())
        );
        verify(1, postRequestedFor(urlEqualTo("/inventory/releases")));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void cancelOrder_orderNotExists_notFoundReturnedWithoutOutboundCalls() throws Exception {
        //given
        long orderId = 999999L;
        //when //then
        mockMvc.perform(post("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order with id = 999999 doesn't exist"));
        verify(0, anyRequestedFor(anyUrl()));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }
}
