package pl.shop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.order.dto.request.AddressRequest;
import pl.shop.order.dto.request.CustomerRequest;
import pl.shop.order.dto.request.PaginationRequest;
import pl.shop.order.dto.request.PlaceOrderRequest;
import pl.shop.order.dto.response.AddressResponse;
import pl.shop.order.dto.response.CustomerResponse;
import pl.shop.order.dto.response.OrderItemComponentResponse;
import pl.shop.order.dto.response.OrderItemResponse;
import pl.shop.order.dto.response.OrderResponse;
import pl.shop.order.dto.response.OrderSummaryResponse;
import pl.shop.order.dto.response.OrderTotalResponse;
import pl.shop.order.dto.response.PageResponse;
import pl.shop.order.dto.response.PriceResponse;
import pl.shop.order.exception.InvalidOrderStatusException;
import pl.shop.order.exception.OrderAlreadyExistsException;
import pl.shop.order.exception.OrderNotFoundException;
import pl.shop.order.model.OrderStatus;
import pl.shop.order.model.SortDirection;
import pl.shop.order.service.OrderService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private OrderService orderService;

    @Test
    void placeOrder_dataCorrect_returnsCreatedOrder() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        PriceResponse unitPrice = new PriceResponse(new BigDecimal("5855.00"), new BigDecimal("0.23"),
                new BigDecimal("1346.65"), new BigDecimal("7201.65"), "PLN");
        OrderResponse response = new OrderResponse(10L, "3f6c2a1e-8b4d-4c1a-9e2f-7d5b0a9c1e42", 1L, OrderStatus.PLACED,
                new CustomerResponse("Jan", "Kowalski", "jan@example.com"),
                new AddressResponse("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new OrderItemResponse(100L, 5L, "Gaming PC Pro", 1, unitPrice, unitPrice,
                        List.of(new OrderItemComponentResponse(2L, 2L, "Graphics card", "RTX 4070")))),
                new OrderTotalResponse(new BigDecimal("5855.00"), new BigDecimal("1346.65"), new BigDecimal("7201.65"), "PLN"),
                LocalDateTime.of(2026, 11, 2, 12, 0), null);
        ArgumentCaptor<PlaceOrderRequest> requestCaptor = ArgumentCaptor.forClass(PlaceOrderRequest.class);
        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenReturn(response);
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.orderNumber").value("3f6c2a1e-8b4d-4c1a-9e2f-7d5b0a9c1e42"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.customer.firstName").value("Jan"))
                .andExpect(jsonPath("$.shippingAddress.postalCode").value("80-001"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].components[0].componentName").value("RTX 4070"))
                .andExpect(jsonPath("$.total.grossPrice").value(7201.65));
        Mockito.verify(orderService).placeOrder(requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, requestCaptor.getValue().cartId()),
                () -> Assertions.assertEquals("jan@example.com", requestCaptor.getValue().customer().email()),
                () -> Assertions.assertEquals("Gdańsk", requestCaptor.getValue().shippingAddress().city())
        );
    }

    @Test
    void placeOrder_cartIdNull_returnsBadRequestWithMessage() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(null,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ARGUMENT_NOT_VALID"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Cart ID is required")));
        Mockito.verify(orderService, Mockito.never()).placeOrder(any());
    }

    @Test
    void placeOrder_emailInvalid_returnsBadRequestWithMessage() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "not-an-email"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("customer.email")));
        Mockito.verify(orderService, Mockito.never()).placeOrder(any());
    }

    @Test
    void placeOrder_postalCodeInvalid_returnsBadRequestWithMessage() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80001", "PL"));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("Postal code must match the XX-XXX format")));
        Mockito.verify(orderService, Mockito.never()).placeOrder(any());
    }

    @Test
    void placeOrder_customerMissing_returnsBadRequestWithMessage() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L, null,
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("Customer details are required")));
    }

    @Test
    void placeOrder_orderAlreadyExists_returnsConflictWithMessage() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenThrow(new OrderAlreadyExistsException(1L));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORDER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Order for cart with id = 1 already exists"));
    }

    @Test
    void placeOrder_inventoryReturnsConflict_returnsConflictWithRemoteError() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        Request feignRequest = Request.create(Request.HttpMethod.POST, "http://localhost:8082/inventory/reservations",
                Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] remoteBody = "{\"status\":409,\"errorCode\":\"INSUFFICIENT_STOCK\",\"message\":\"Insufficient stock for product with id = 2. Requested: 3, available: 2\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(orderService.placeOrder(any(PlaceOrderRequest.class)))
                .thenThrow(new FeignException.Conflict("Conflict", feignRequest, remoteBody, null));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product with id = 2. Requested: 3, available: 2"));
    }

    @Test
    void placeOrder_remoteServiceUnavailable_returnsServiceUnavailable() throws Exception {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        Request feignRequest = Request.create(Request.HttpMethod.GET, "http://localhost:8083/carts/1",
                Map.of(), null, StandardCharsets.UTF_8, null);
        when(orderService.placeOrder(any(PlaceOrderRequest.class)))
                .thenThrow(new RetryableException(503, "Service Unavailable", Request.HttpMethod.GET, (Long) null, feignRequest));
        //when //then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void getOrder_orderNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        when(orderService.getOrder(999L)).thenThrow(new OrderNotFoundException(999L));
        //when //then
        mockMvc.perform(get("/orders/{orderId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order with id = 999 doesn't exist"));
    }

    @Test
    void getOrders_emailAndPaginationGiven_passedToService() throws Exception {
        //given
        PageResponse<OrderSummaryResponse> page = new PageResponse<>(List.of(
                new OrderSummaryResponse(10L, "3f6c2a1e-8b4d-4c1a-9e2f-7d5b0a9c1e42", OrderStatus.PLACED, 1,
                        new BigDecimal("7201.65"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0))
        ), 0, 5, 1L, 1);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaginationRequest> paginationCaptor = ArgumentCaptor.forClass(PaginationRequest.class);
        when(orderService.getOrders(any(), any(PaginationRequest.class))).thenReturn(page);
        //when //then
        mockMvc.perform(get("/orders")
                        .param("email", "jan@example.com")
                        .param("size", "5")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].itemCount").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
        Mockito.verify(orderService).getOrders(emailCaptor.capture(), paginationCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("jan@example.com", emailCaptor.getValue()),
                () -> Assertions.assertEquals(0, paginationCaptor.getValue().page()),
                () -> Assertions.assertEquals(5, paginationCaptor.getValue().size()),
                () -> Assertions.assertEquals("createdAt", paginationCaptor.getValue().sortBy()),
                () -> Assertions.assertEquals(SortDirection.DESC, paginationCaptor.getValue().direction())
        );
    }

    @Test
    void getOrders_pageSizeTooBig_returnsBadRequest() throws Exception {
        //given
        String size = "101";
        //when //then
        mockMvc.perform(get("/orders").param("size", size))
                .andExpect(status().isBadRequest());
        Mockito.verify(orderService, Mockito.never()).getOrders(any(), any());
    }

    @Test
    void cancelOrder_orderAlreadyCancelled_returnsConflict() throws Exception {
        //given
        when(orderService.cancelOrder(anyLong())).thenThrow(new InvalidOrderStatusException(10L, OrderStatus.CANCELLED));
        //when //then
        mockMvc.perform(post("/orders/{orderId}/cancel", 10L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATUS"))
                .andExpect(jsonPath("$.message").value("Order with id = 10 cannot be modified in status CANCELLED"));
    }

    @Test
    void cancelOrder_orderNotExists_returnsNotFound() throws Exception {
        //given
        when(orderService.cancelOrder(999L)).thenThrow(new OrderNotFoundException(999L));
        //when //then
        mockMvc.perform(post("/orders/{orderId}/cancel", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"));
    }
}
