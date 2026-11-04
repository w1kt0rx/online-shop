package pl.shop.invoice.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.shop.invoice.dto.request.PaginationRequest;
import pl.shop.invoice.dto.response.AddressResponse;
import pl.shop.invoice.dto.response.BuyerResponse;
import pl.shop.invoice.dto.response.InvoiceLineResponse;
import pl.shop.invoice.dto.response.InvoiceResponse;
import pl.shop.invoice.dto.response.InvoiceSummaryResponse;
import pl.shop.invoice.dto.response.PageResponse;
import pl.shop.invoice.exception.InvoiceNotFoundException;
import pl.shop.invoice.model.InvoiceStatus;
import pl.shop.invoice.model.SortDirection;
import pl.shop.invoice.service.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
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
        InvoiceResponse response = new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", InvoiceStatus.ISSUED,
                new BuyerResponse("Jan", "Kowalski", "jan@example.com"),
                new AddressResponse("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(new InvoiceLineResponse(100L, "Headphones ABC", null, 2, new BigDecimal("240.00"), new BigDecimal("0.23"),
                        new BigDecimal("480.00"), new BigDecimal("110.40"), new BigDecimal("590.40"))),
                new BigDecimal("480.00"), new BigDecimal("110.40"), new BigDecimal("590.40"), "PLN",
                LocalDateTime.of(2026, 11, 2, 12, 0), null);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        when(invoiceService.getInvoice(1L)).thenReturn(response);
        //when //then
        mockMvc.perform(get("/invoices/{invoiceId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("FV/2026/11/000001"))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.buyer.email").value("jan@example.com"))
                .andExpect(jsonPath("$.billingAddress.city").value("Gdańsk"))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].grossAmount").value(590.40))
                .andExpect(jsonPath("$.totalGross").value(590.40));
        Mockito.verify(invoiceService).getInvoice(idCaptor.capture());
        Assertions.assertEquals(1L, idCaptor.getValue());
    }

    @Test
    void getInvoice_invoiceNotExists_returnsNotFoundWithMessage() throws Exception {
        //given
        when(invoiceService.getInvoice(999L)).thenThrow(InvoiceNotFoundException.byId(999L));
        //when //then
        mockMvc.perform(get("/invoices/{invoiceId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVOICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invoice with id = 999 doesn't exist"));
    }

    @Test
    void getInvoice_idNotNumber_returnsBadRequest() throws Exception {
        //given
        String invoiceId = "abc";
        //when //then
        mockMvc.perform(get("/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message", Matchers.containsString("invoiceId")));
    }

    @Test
    void getInvoiceByOrder_invoiceExists_returnsInvoice() throws Exception {
        //given
        InvoiceResponse response = new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", InvoiceStatus.CANCELLED,
                new BuyerResponse("Jan", "Kowalski", "jan@example.com"),
                new AddressResponse("Długa 5/3", "Gdańsk", "80-001", "PL"), List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN",
                LocalDateTime.of(2026, 11, 2, 12, 0), LocalDateTime.of(2026, 11, 3, 9, 0));
        ArgumentCaptor<Long> orderIdCaptor = ArgumentCaptor.forClass(Long.class);
        when(invoiceService.getInvoiceByOrder(10L)).thenReturn(response);
        //when //then
        mockMvc.perform(get("/invoices/orders/{orderId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10L))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());
        Mockito.verify(invoiceService).getInvoiceByOrder(orderIdCaptor.capture());
        Assertions.assertEquals(10L, orderIdCaptor.getValue());
    }

    @Test
    void getInvoiceByOrder_invoiceNotExists_returnsNotFound() throws Exception {
        //given
        when(invoiceService.getInvoiceByOrder(999L)).thenThrow(InvoiceNotFoundException.forOrder(999L));
        //when //then
        mockMvc.perform(get("/invoices/orders/{orderId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice for order with id = 999 doesn't exist"));
    }

    @Test
    void getInvoices_emailAndPaginationGiven_passedToService() throws Exception {
        //given
        PageResponse<InvoiceSummaryResponse> page = new PageResponse<>(List.of(
                new InvoiceSummaryResponse(1L, "FV/2026/11/000001", "order-1", InvoiceStatus.ISSUED,
                        new BigDecimal("590.40"), "PLN", LocalDateTime.of(2026, 11, 2, 12, 0))
        ), 0, 5, 1L, 1);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaginationRequest> paginationCaptor = ArgumentCaptor.forClass(PaginationRequest.class);
        when(invoiceService.getInvoices(any(), any(PaginationRequest.class))).thenReturn(page);
        //when //then
        mockMvc.perform(get("/invoices")
                        .param("email", "jan@example.com")
                        .param("size", "5")
                        .param("sortBy", "issuedAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("FV/2026/11/000001"))
                .andExpect(jsonPath("$.totalElements").value(1));
        Mockito.verify(invoiceService).getInvoices(emailCaptor.capture(), paginationCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals("jan@example.com", emailCaptor.getValue()),
                () -> Assertions.assertEquals(5, paginationCaptor.getValue().size()),
                () -> Assertions.assertEquals("issuedAt", paginationCaptor.getValue().sortBy()),
                () -> Assertions.assertEquals(SortDirection.DESC, paginationCaptor.getValue().direction())
        );
    }

    @Test
    void getInvoices_pageNegative_returnsBadRequest() throws Exception {
        //given
        String page = "-1";
        //when //then
        mockMvc.perform(get("/invoices").param("page", page))
                .andExpect(status().isBadRequest());
        Mockito.verify(invoiceService, Mockito.never()).getInvoices(any(), any());
    }
}
