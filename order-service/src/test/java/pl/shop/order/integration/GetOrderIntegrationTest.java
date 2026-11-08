package pl.shop.order.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import pl.shop.order.model.Address;
import pl.shop.order.model.Customer;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItemComponent;
import pl.shop.order.model.Price;
import pl.shop.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class GetOrderIntegrationTest {
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
    void getOrder_orderExists_orderWithItemsAndTotalsReturned() throws Exception {
        //given
        Order order = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        order.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2, List.of(
                new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070"),
                new OrderItemComponent(4L, 4L, "RAM", "RAM 32 GB DDR5")
        ));
        Order saved = orderRepository.save(order);
        //when //then
        mockMvc.perform(get("/orders/{orderId}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.orderNumber").value(saved.getOrderNumber()))
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.customer.firstName").value("Jan"))
                .andExpect(jsonPath("$.customer.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.customer.email").value("jan@example.com"))
                .andExpect(jsonPath("$.shippingAddress.postalCode").value("80-001"))
                .andExpect(jsonPath("$.shippingAddress.country").value("PL"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(5))
                .andExpect(jsonPath("$.items[0].productName").value("Gaming PC Pro"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice.netPrice").value(5855.00))
                .andExpect(jsonPath("$.items[0].unitPrice.vatRate").value(0.23))
                .andExpect(jsonPath("$.items[0].unitPrice.vatAmount").value(1346.65))
                .andExpect(jsonPath("$.items[0].unitPrice.grossPrice").value(7201.65))
                .andExpect(jsonPath("$.items[0].totalPrice.netPrice").value(11710.00))
                .andExpect(jsonPath("$.items[0].components", hasSize(2)))
                .andExpect(jsonPath("$.items[0].components[0].componentProductId").value(2))
                .andExpect(jsonPath("$.items[0].components[0].slot").value("Graphics card"))
                .andExpect(jsonPath("$.items[0].components[1].componentName").value("RAM 32 GB DDR5"))
                .andExpect(jsonPath("$.total.netPrice").value(11710.00))
                .andExpect(jsonPath("$.total.vatAmount").value(2693.30))
                .andExpect(jsonPath("$.total.grossPrice").value(14403.30))
                .andExpect(jsonPath("$.total.currency").value("PLN"));
        verify(0, anyRequestedFor(anyUrl()));
        Mockito.verify(kafkaTemplate, Mockito.never()).send(any(), any(), any());
    }

    @Test
    void getOrder_orderNotExists_notFoundReturned() throws Exception {
        //given
        long orderId = 999999L;
        //when //then
        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order with id = 999999 doesn't exist"));
    }

    @Test
    void getOrders_emailGivenWithPaging_onlyCustomerOrdersReturnedPaged() throws Exception {
        //given
        Order firstOrder = Order.place(1L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        firstOrder.addItem(5L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 1, List.of(
                new OrderItemComponent(2L, 2L, "Graphics card", "RTX 4070")
        ));
        firstOrder.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Order secondOrder = Order.place(2L, new Customer("Jan", "Kowalski", "jan@example.com"),
                new Address("Długa 5/3", "Gdańsk", "80-001", "PL"));
        secondOrder.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Order otherCustomerOrder = Order.place(3L, new Customer("Anna", "Nowak", "anna@example.com"),
                new Address("Krótka 1", "Warszawa", "00-001", "PL"));
        otherCustomerOrder.addItem(2L, "RTX 4070", new Price(new BigDecimal("2275.00"), new BigDecimal("0.23"), "PLN"), 1, List.of());
        Order savedFirst = orderRepository.save(firstOrder);
        orderRepository.save(secondOrder);
        orderRepository.save(otherCustomerOrder);
        //when //then
        mockMvc.perform(get("/orders")
                        .param("email", "JAN@example.com")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "id")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(savedFirst.getId()))
                .andExpect(jsonPath("$.content[0].orderNumber").value(savedFirst.getOrderNumber()))
                .andExpect(jsonPath("$.content[0].status").value("PLACED"))
                .andExpect(jsonPath("$.content[0].itemCount").value(2))
                .andExpect(jsonPath("$.content[0].grossPrice").value(9999.90))
                .andExpect(jsonPath("$.content[0].currency").value("PLN"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void getOrders_pageSizeTooBig_badRequestReturned() throws Exception {
        //given
        String size = "101";
        //when //then
        mockMvc.perform(get("/orders").param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Page size cannot exceed 100")));
        verify(0, anyRequestedFor(anyUrl()));
    }
}
