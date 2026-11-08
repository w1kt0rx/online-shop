package pl.shop.interaction.controller;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.InvoiceSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.service.InvoiceService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InvoiceControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void getInvoice_invoiceExists_returnsInvoice() throws Exception {
        //given
        when(invoiceService.getInvoice(1L)).thenReturn(new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", "ISSUED",
                null, null, List.of(), null, null, null, "PLN", null, null));
        //when //then
        mockMvc.perform(get("/api/invoices/{invoiceId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("FV/2026/11/000001"));
    }

    @Test
    void getInvoiceByOrder_invoiceNotFound_returnsNotFoundWithRemoteError() throws Exception {
        //given
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost:8085/invoices/orders/999", Map.of(), null, StandardCharsets.UTF_8, null);
        byte[] body = "{\"status\":404,\"errorCode\":\"INVOICE_NOT_FOUND\",\"message\":\"Invoice for order with id = 999 doesn't exist\"}"
                .getBytes(StandardCharsets.UTF_8);
        when(invoiceService.getInvoiceByOrder(999L)).thenThrow(new FeignException.NotFound("Not Found", request, body, null));
        //when //then
        mockMvc.perform(get("/api/invoices/orders/{orderId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVOICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invoice for order with id = 999 doesn't exist"));
    }

    @Test
    void getInvoices_emailGiven_passedToService() throws Exception {
        //given
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaginationRequest> paginationCaptor = ArgumentCaptor.forClass(PaginationRequest.class);
        when(invoiceService.getInvoices(any(), any(PaginationRequest.class)))
                .thenReturn(new PageResponse<InvoiceSummaryResponse>(List.of(), 0, 20, 0L, 0));
        //when //then
        mockMvc.perform(get("/api/invoices").param("email", "jan@example.com").param("page", "0"))
                .andExpect(status().isOk());
        Mockito.verify(invoiceService).getInvoices(emailCaptor.capture(), paginationCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("jan@example.com", emailCaptor.getValue()),
                () -> Assertions.assertEquals(0, paginationCaptor.getValue().page())
        );
    }

    @Test
    void getInvoice_idNotNumber_returnsBadRequest() throws Exception {
        //given
        String invoiceId = "abc";
        //when //then
        mockMvc.perform(get("/api/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }
}
