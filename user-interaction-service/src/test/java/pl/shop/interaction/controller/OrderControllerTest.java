package pl.shop.interaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
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
import pl.shop.interaction.dto.request.AddressRequest;
import pl.shop.interaction.dto.request.CustomerRequest;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.PlaceOrderRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.OrderDetailsResponse;
import pl.shop.interaction.dto.response.OrderResponse;
import pl.shop.interaction.dto.response.OrderSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.service.OrderService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
        ArgumentCaptor<PlaceOrderRequest> requestCaptor = ArgumentCaptor.forClass(PlaceOrderRequest.class);
        when(orderService.placeOrder(any(PlaceOrderRequest.class)))
                .thenReturn(new OrderResponse(10L, "order-1", 1L, "PLACED", null, null, List.of(), null, null, null));
        //when //then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("PLACED"));
        Mockito.verify(orderService).placeOrder(requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, requestCaptor.getValue().cartId()),
                () -> Assertions.assertEquals("Gdańsk", requestCaptor.getValue().shippingAddress().city())
        );
    }

    @Test
    void placeOrder_inventoryConflictInOrderService_returnsConflictWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.POST, "http://localhost:8084/orders", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":409,\"errorCode\":\"INSUFFICIENT_STOCK\",\"message\":\"Insufficient stock for product with id = 2. Requested: 3, available: 2\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(orderService.placeOrder(any(PlaceOrderRequest.class))).thenThrow(new FeignException.Conflict("Conflict", request, body, null));
        //when //then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void getOrder_orderWithInvoice_returnsDetails() throws Exception {
        //given
        OrderDetailsResponse details = new OrderDetailsResponse(
                new OrderResponse(10L, "order-1", 1L, "PLACED", null, null, List.of(), null, null, null),
                new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", "ISSUED", null, null, List.of(), null, null, null, "PLN", null, null));
        when(orderService.getOrderDetails(10L)).thenReturn(details);
        //when //then
        mockMvc.perform(get("/api/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.orderNumber").value("order-1"))
                .andExpect(jsonPath("$.invoice.invoiceNumber").value("FV/2026/11/000001"));
    }

    @Test
    void getOrder_invoiceNotIssuedYet_returnsDetailsWithNullInvoice() throws Exception {
        //given
        OrderDetailsResponse details = new OrderDetailsResponse(
                new OrderResponse(10L, "order-1", 1L, "PLACED", null, null, List.of(), null, null, null), null);
        when(orderService.getOrderDetails(10L)).thenReturn(details);
        //when //then
        mockMvc.perform(get("/api/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id").value(10L))
                .andExpect(jsonPath("$.invoice").isEmpty());
    }

    @Test
    void getOrders_emailGiven_passedToService() throws Exception {
        //given
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaginationRequest> paginationCaptor = ArgumentCaptor.forClass(PaginationRequest.class);
        when(orderService.getOrders(any(), any(PaginationRequest.class)))
                .thenReturn(new PageResponse<OrderSummaryResponse>(List.of(), 0, 20, 0L, 0));
        //when //then
        mockMvc.perform(get("/api/orders").param("email", "jan@example.com").param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        Mockito.verify(orderService).getOrders(emailCaptor.capture(), paginationCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("jan@example.com", emailCaptor.getValue()),
                () -> Assertions.assertEquals("DESC", paginationCaptor.getValue().direction())
        );
    }

    @Test
    void cancelOrder_alreadyCancelledInOrderService_returnsConflict() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.POST, "http://localhost:8084/orders/10/cancel", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":409,\"errorCode\":\"INVALID_ORDER_STATUS\",\"message\":\"Order with id = 10 cannot be modified in status CANCELLED\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(orderService.cancelOrder(anyLong())).thenThrow(new FeignException.Conflict("Conflict", request, body, null));
        //when //then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 10L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void cancelOrder_orderServiceReturns500_returnsServiceUnavailable() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.POST, "http://localhost:8084/orders/10/cancel", Map.of(), null, StandardCharsets.UTF_8, null);
        when(orderService.cancelOrder(anyLong())).thenThrow(new FeignException.InternalServerError("Internal Server Error", request, null, null));
        //when //then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", 10L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("REMOTE_SERVICE_ERROR"));
    }
}
