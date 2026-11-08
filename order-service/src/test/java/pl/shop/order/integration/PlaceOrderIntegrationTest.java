package pl.shop.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.wiremock.spring.EnableWireMock;
import pl.shop.order.dto.request.AddressRequest;
import pl.shop.order.dto.request.CustomerRequest;
import pl.shop.order.dto.request.PlaceOrderRequest;
import pl.shop.order.kafka.event.OrderPlacedEvent;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItem;
import pl.shop.order.model.OrderItemComponent;
import pl.shop.order.model.OrderStatus;
import pl.shop.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class PlaceOrderIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        orderRepository.deleteAll();
    }

    @Test
    void placeOrder_cartActiveAndStockAvailable_orderPersistedStockReservedAndEventSent() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "status": "ACTIVE",
                                  "items": [
                                    {
                                      "id": 100,
                                      "productId": 5,
                                      "productName": "Gaming PC Pro",
                                      "quantity": 2,
                                      "unitPrice": {"netPrice": 5855.00, "vatRate": 0.23, "vatAmount": 1346.65, "grossPrice": 7201.65, "currency": "PLN"},
                                      "totalPrice": {"netPrice": 11710.00, "vatRate": 0.23, "vatAmount": 2693.30, "grossPrice": 14403.30, "currency": "PLN"},
                                      "components": [
                                        {"configurationId": 2, "componentProductId": 2, "slot": "Graphics card", "componentName": "RTX 4070"},
                                        {"configurationId": 4, "componentProductId": 4, "slot": "RAM", "componentName": "RAM 32 GB DDR5"}
                                      ]
                                    },
                                    {
                                      "id": 101,
                                      "productId": 2,
                                      "productName": "RTX 4070",
                                      "quantity": 1,
                                      "unitPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "totalPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "components": []
                                    }
                                  ],
                                  "total": {"netPrice": 13985.00, "vatAmount": 3216.55, "grossPrice": 17201.55, "currency": "PLN"}
                                }
                                """)));
        stubFor(WireMock.post(urlEqualTo("/inventory/reservations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"productId": 5, "quantity": 8, "available": true},
                                  {"productId": 2, "quantity": 7, "available": true},
                                  {"productId": 4, "quantity": 18, "available": true}
                                ]
                                """)));
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        //when
        MvcResult result = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.customer.firstName").value("Jan"))
                .andExpect(jsonPath("$.customer.email").value("jan@example.com"))
                .andExpect(jsonPath("$.shippingAddress.postalCode").value("80-001"))
                .andExpect(jsonPath("$.shippingAddress.country").value("PL"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productId").value(5))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(11710.00))
                .andExpect(jsonPath("$.items[0].components", hasSize(2)))
                .andExpect(jsonPath("$.items[0].components[0].componentName").value("RTX 4070"))
                .andExpect(jsonPath("$.items[1].productId").value(2))
                .andExpect(jsonPath("$.items[1].components", hasSize(0)))
                .andExpect(jsonPath("$.total.netPrice").value(13985.00))
                .andExpect(jsonPath("$.total.vatAmount").value(3216.55))
                .andExpect(jsonPath("$.total.grossPrice").value(17201.55))
                .andExpect(jsonPath("$.total.currency").value("PLN"))
                .andReturn();
        //then
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long orderId = body.get("id").asLong();
        String orderNumber = body.get("orderNumber").asText();
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Order saved = orderRepository.findById(orderId).orElseThrow();
            OrderItem firstItem = saved.getItems().get(0);
            OrderItem secondItem = saved.getItems().get(1);
            OrderItemComponent firstComponent = firstItem.getComponents().get(0);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1L, orderRepository.count()),
                    () -> Assertions.assertEquals(orderNumber, saved.getOrderNumber()),
                    () -> Assertions.assertEquals(1L, saved.getCartId()),
                    () -> Assertions.assertEquals(OrderStatus.PLACED, saved.getStatus()),
                    () -> Assertions.assertNotNull(saved.getCreatedAt()),
                    () -> Assertions.assertNull(saved.getCancelledAt()),
                    () -> Assertions.assertEquals("Jan", saved.getCustomer().getFirstName()),
                    () -> Assertions.assertEquals("Kowalski", saved.getCustomer().getLastName()),
                    () -> Assertions.assertEquals("jan@example.com", saved.getCustomer().getEmail()),
                    () -> Assertions.assertEquals("Długa 5/3", saved.getShippingAddress().getStreet()),
                    () -> Assertions.assertEquals("Gdańsk", saved.getShippingAddress().getCity()),
                    () -> Assertions.assertEquals("80-001", saved.getShippingAddress().getPostalCode()),
                    () -> Assertions.assertEquals("PL", saved.getShippingAddress().getCountry()),
                    () -> Assertions.assertEquals(2, saved.getItems().size()),
                    () -> Assertions.assertEquals(5L, firstItem.getProductId()),
                    () -> Assertions.assertEquals("Gaming PC Pro", firstItem.getProductName()),
                    () -> Assertions.assertEquals(2, firstItem.getQuantity()),
                    () -> Assertions.assertEquals(0, new BigDecimal("5855.00").compareTo(firstItem.getUnitPrice().getNetPrice())),
                    () -> Assertions.assertEquals(0, new BigDecimal("0.23").compareTo(firstItem.getUnitPrice().getVatRate())),
                    () -> Assertions.assertEquals("PLN", firstItem.getUnitPrice().getCurrency()),
                    () -> Assertions.assertEquals(2, firstItem.getComponents().size()),
                    () -> Assertions.assertEquals(2L, firstComponent.getConfigurationId()),
                    () -> Assertions.assertEquals(2L, firstComponent.getComponentProductId()),
                    () -> Assertions.assertEquals("Graphics card", firstComponent.getSlot()),
                    () -> Assertions.assertEquals("RTX 4070", firstComponent.getComponentName()),
                    () -> Assertions.assertEquals(2L, secondItem.getProductId()),
                    () -> Assertions.assertEquals(1, secondItem.getQuantity()),
                    () -> Assertions.assertEquals(0, new BigDecimal("2275.00").compareTo(secondItem.getUnitPrice().getNetPrice())),
                    () -> Assertions.assertTrue(secondItem.getComponents().isEmpty()),
                    () -> Assertions.assertEquals(0, new BigDecimal("13985.00").compareTo(saved.getTotalNet())),
                    () -> Assertions.assertEquals(0, new BigDecimal("3216.55").compareTo(saved.getTotalVat())),
                    () -> Assertions.assertEquals(0, new BigDecimal("17201.55").compareTo(saved.getTotalGross()))
            );
        });
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
        verify(1, postRequestedFor(urlEqualTo("/inventory/reservations"))
                .withRequestBody(equalToJson("""
                        {
                          "orderNumber": "%s",
                          "items": [
                            {"productId": 5, "quantity": 2},
                            {"productId": 2, "quantity": 3},
                            {"productId": 4, "quantity": 2}
                          ]
                        }
                        """.formatted(orderNumber))));
        Mockito.verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        OrderPlacedEvent event = (OrderPlacedEvent) eventCaptor.getValue();
        Assertions.assertAll(
                () -> Assertions.assertEquals("order-placed", topicCaptor.getValue()),
                () -> Assertions.assertEquals(orderNumber, keyCaptor.getValue()),
                () -> Assertions.assertEquals(orderId, event.orderId()),
                () -> Assertions.assertEquals(orderNumber, event.orderNumber()),
                () -> Assertions.assertEquals(1L, event.cartId()),
                () -> Assertions.assertNotNull(event.placedAt()),
                () -> Assertions.assertEquals(new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"), event.customer()),
                () -> Assertions.assertEquals(new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"), event.shippingAddress()),
                () -> Assertions.assertEquals(2, event.items().size()),
                () -> Assertions.assertEquals(5L, event.items().get(0).productId()),
                () -> Assertions.assertEquals("Gaming PC Pro", event.items().get(0).productName()),
                () -> Assertions.assertEquals(2, event.items().get(0).quantity()),
                () -> Assertions.assertEquals(new BigDecimal("5855.00"), event.items().get(0).unitNetPrice()),
                () -> Assertions.assertEquals(new BigDecimal("0.23"), event.items().get(0).vatRate()),
                () -> Assertions.assertEquals("PLN", event.items().get(0).currency()),
                () -> Assertions.assertEquals(List.of("Graphics card: RTX 4070", "RAM: RAM 32 GB DDR5"), event.items().get(0).components()),
                () -> Assertions.assertEquals(2L, event.items().get(1).productId()),
                () -> Assertions.assertEquals(List.of(), event.items().get(1).components()),
                () -> Assertions.assertEquals(new BigDecimal("13985.00"), event.totalNet()),
                () -> Assertions.assertEquals(new BigDecimal("3216.55"), event.totalVat()),
                () -> Assertions.assertEquals(new BigDecimal("17201.55"), event.totalGross()),
                () -> Assertions.assertEquals("PLN", event.currency())
        );
    }

    @Test
    void placeOrder_inventoryReturnsInsufficientStock_conflictPassedThroughAndOrderRolledBack() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "status": "ACTIVE",
                                  "items": [
                                    {
                                      "id": 101,
                                      "productId": 2,
                                      "productName": "RTX 4070",
                                      "quantity": 3,
                                      "unitPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "totalPrice": {"netPrice": 6825.00, "vatRate": 0.23, "vatAmount": 1569.75, "grossPrice": 8394.75, "currency": "PLN"},
                                      "components": []
                                    }
                                  ],
                                  "total": {"netPrice": 6825.00, "vatAmount": 1569.75, "grossPrice": 8394.75, "currency": "PLN"}
                                }
                                """)));
        stubFor(WireMock.post(urlEqualTo("/inventory/reservations"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-02T12:00:00", "status": 409, "errorCode": "INSUFFICIENT_STOCK", "message": "Insufficient stock for product with id = 2. Requested: 3, available: 2"}
                                """)));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 2. Requested: 3, available: 2"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(0L, orderRepository.count()),
                () -> Assertions.assertFalse(orderRepository.existsByCartId(1L))
        );
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
        verify(1, postRequestedFor(urlEqualTo("/inventory/reservations"))
                .withRequestBody(equalToJson("""
                        {"items": [{"productId": 2, "quantity": 3}]}
                        """, false, true)));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void placeOrder_cartNotFound_notFoundPassedThroughAndNothingReserved() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(999L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"timestamp": "2026-11-02T12:00:00", "status": 404, "errorCode": "CART_NOT_FOUND", "message": "Cart with id = 999 doesn't exist"}
                                """)));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("CART_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cart with id = 999 doesn't exist"));
        Assertions.assertEquals(0L, orderRepository.count());
        verify(1, getRequestedFor(urlEqualTo("/carts/999")));
        verify(0, postRequestedFor(urlEqualTo("/inventory/reservations")));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void placeOrder_cartNotActive_conflictReturnedAndNothingReserved() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "status": "ORDERED",
                                  "items": [
                                    {
                                      "id": 101,
                                      "productId": 2,
                                      "productName": "RTX 4070",
                                      "quantity": 1,
                                      "unitPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "totalPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "components": []
                                    }
                                  ],
                                  "total": {"netPrice": 2275.00, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"}
                                }
                                """)));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_NOT_ACTIVE"))
                .andExpect(jsonPath("$.message").value("Cart with id = 1 is not active"));
        Assertions.assertEquals(0L, orderRepository.count());
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
        verify(0, postRequestedFor(urlEqualTo("/inventory/reservations")));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void placeOrder_cartEmpty_conflictReturnedAndNothingReserved() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "status": "ACTIVE",
                                  "items": [],
                                  "total": {"netPrice": 0.00, "vatAmount": 0.00, "grossPrice": 0.00, "currency": "PLN"}
                                }
                                """)));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_EMPTY"))
                .andExpect(jsonPath("$.message").value("Cart with id = 1 is empty"));
        Assertions.assertEquals(0L, orderRepository.count());
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
        verify(0, postRequestedFor(urlEqualTo("/inventory/reservations")));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void placeOrder_orderForCartAlreadyPlaced_conflictReturnedWithoutOutboundCalls() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "status": "ACTIVE",
                                  "items": [
                                    {
                                      "id": 101,
                                      "productId": 2,
                                      "productName": "RTX 4070",
                                      "quantity": 1,
                                      "unitPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "totalPrice": {"netPrice": 2275.00, "vatRate": 0.23, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"},
                                      "components": []
                                    }
                                  ],
                                  "total": {"netPrice": 2275.00, "vatAmount": 523.25, "grossPrice": 2798.25, "currency": "PLN"}
                                }
                                """)));
        stubFor(WireMock.post(urlEqualTo("/inventory/reservations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"productId": 2, "quantity": 9, "available": true}]
                                """)));
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Order for cart with id = 1 already exists"));
        Assertions.assertEquals(1L, orderRepository.count());
        verify(1, getRequestedFor(urlEqualTo("/carts/1")));
        verify(1, postRequestedFor(urlEqualTo("/inventory/reservations")));
        Mockito.verify(kafkaTemplate, Mockito.times(1)).send(any(), any(), any());
    }

    @Test
    void placeOrder_cartServiceUnavailable_retriedFiveTimesAndServiceUnavailableReturned() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        stubFor(WireMock.get(urlEqualTo("/carts/1"))
                .willReturn(aResponse().withStatus(503)));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Remote service is unavailable, try again later"));
        Assertions.assertEquals(0L, orderRepository.count());
        verify(5, getRequestedFor(urlEqualTo("/carts/1")));
        verify(0, postRequestedFor(urlEqualTo("/inventory/reservations")));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void placeOrder_emailInvalid_badRequestReturnedWithoutOutboundCalls() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "not-an-email"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("customer.email")))
                .andExpect(jsonPath("$.message", Matchers.containsString("Email address must be a valid email format")));
        Assertions.assertEquals(0L, orderRepository.count());
        verify(0, anyRequestedFor(anyUrl()));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void placeOrder_postalCodeInvalid_badRequestReturnedWithoutOutboundCalls() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80001", "PL"));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("shippingAddress.postalCode")))
                .andExpect(jsonPath("$.message", Matchers.containsString("Postal code must match the XX-XXX format")));
        Assertions.assertEquals(0L, orderRepository.count());
        verify(0, anyRequestedFor(anyUrl()));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }
}
