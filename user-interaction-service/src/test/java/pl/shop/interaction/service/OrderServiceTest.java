package pl.shop.interaction.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.interaction.client.InvoiceServiceClient;
import pl.shop.interaction.client.OrderServiceClient;
import pl.shop.interaction.dto.request.AddressRequest;
import pl.shop.interaction.dto.request.CustomerRequest;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.PlaceOrderRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.OrderDetailsResponse;
import pl.shop.interaction.dto.response.OrderResponse;
import pl.shop.interaction.dto.response.OrderSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OrderServiceTest {
    private OrderService orderService;
    private OrderServiceClient orderServiceClient;
    private InvoiceServiceClient invoiceServiceClient;

    @BeforeEach
    void setup() {
        this.orderServiceClient = Mockito.mock(OrderServiceClient.class);
        this.invoiceServiceClient = Mockito.mock(InvoiceServiceClient.class);
        this.orderService = new OrderService(orderServiceClient, invoiceServiceClient);
    }

    @Test
    void placeOrder_requestGiven_passedToOrderServiceAndOrderReturned() {
        //given
        PlaceOrderRequest request = new PlaceOrderRequest(1L,
                new CustomerRequest("Jan", "Kowalski", "jan@example.com"),
                new AddressRequest("Długa 5/3", "Gdańsk", "80-001", "PL"));
        OrderResponse order = new OrderResponse(10L, "order-1", 1L, "PLACED", null, null, List.of(), null, null, null);
        ArgumentCaptor<PlaceOrderRequest> requestCaptor = ArgumentCaptor.forClass(PlaceOrderRequest.class);
        when(orderServiceClient.placeOrder(any(PlaceOrderRequest.class))).thenReturn(order);
        //when
        OrderResponse result = orderService.placeOrder(request);
        //then
        Mockito.verify(orderServiceClient).placeOrder(requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(1L, requestCaptor.getValue().cartId()),
                () -> Assertions.assertEquals("jan@example.com", requestCaptor.getValue().customer().email()),
                () -> Assertions.assertSame(order, result)
        );
        Mockito.verifyNoInteractions(invoiceServiceClient);
    }

    @Test
    void getOrderDetails_invoiceIssued_orderWithInvoiceReturned() {
        //given
        OrderResponse order = new OrderResponse(10L, "order-1", 1L, "PLACED", null, null, List.of(), null, null, null);
        InvoiceResponse invoice = new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", "ISSUED", null, null, List.of(),
                null, null, null, "PLN", null, null);
        when(orderServiceClient.getOrder(10L)).thenReturn(order);
        when(invoiceServiceClient.getInvoiceByOrder(10L)).thenReturn(invoice);
        //when
        OrderDetailsResponse details = orderService.getOrderDetails(10L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertSame(order, details.order()),
                () -> Assertions.assertSame(invoice, details.invoice()),
                () -> Assertions.assertEquals("FV/2026/11/000001", details.invoice().invoiceNumber())
        );
    }

    @Test
    void getOrderDetails_invoiceNotIssuedYet_orderWithoutInvoiceReturned() {
        //given
        OrderResponse order = new OrderResponse(10L, "order-1", 1L, "PLACED", null, null, List.of(), null, null, null);
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8085/invoices/orders/10", Map.of(), null, StandardCharsets.UTF_8, null);
        when(orderServiceClient.getOrder(10L)).thenReturn(order);
        when(invoiceServiceClient.getInvoiceByOrder(10L)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));
        //when
        OrderDetailsResponse details = orderService.getOrderDetails(10L);
        //then
        Assertions.assertAll(
                () -> Assertions.assertSame(order, details.order()),
                () -> Assertions.assertNull(details.invoice())
        );
    }

    @Test
    void getOrderDetails_orderNotFound_exceptionPropagatedAndInvoiceNotFetched() {
        //given
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8084/orders/999", Map.of(), null, StandardCharsets.UTF_8, null);
        when(orderServiceClient.getOrder(999L)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));
        //when //then
        Assertions.assertThrows(
                FeignException.NotFound.class,
                () -> orderService.getOrderDetails(999L)
        );
        Mockito.verifyNoInteractions(invoiceServiceClient);
    }

    @Test
    void getOrders_emailAndPaginationGiven_passedToOrderService() {
        //given
        PageResponse<OrderSummaryResponse> page = new PageResponse<>(List.of(), 0, 20, 0L, 0);
        when(orderServiceClient.getOrders(any(), any(), any(), any(), any())).thenReturn(page);
        //when
        PageResponse<OrderSummaryResponse> result = orderService.getOrders("jan@example.com", new PaginationRequest(0, 20, "createdAt", "DESC"));
        //then
        Mockito.verify(orderServiceClient).getOrders("jan@example.com", 0, 20, "createdAt", "DESC");
        Assertions.assertSame(page, result);
    }

    @Test
    void cancelOrder_orderGiven_passedToOrderService() {
        //given
        OrderResponse cancelled = new OrderResponse(10L, "order-1", 1L, "CANCELLED", null, null, List.of(), null, null, null);
        when(orderServiceClient.cancelOrder(10L)).thenReturn(cancelled);
        //when
        OrderResponse result = orderService.cancelOrder(10L);
        //then
        Assertions.assertEquals("CANCELLED", result.status());
    }
}
