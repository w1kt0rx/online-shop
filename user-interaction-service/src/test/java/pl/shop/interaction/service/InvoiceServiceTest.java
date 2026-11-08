package pl.shop.interaction.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.shop.interaction.client.InvoiceServiceClient;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.InvoiceSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InvoiceServiceTest {
    private InvoiceService invoiceService;
    private InvoiceServiceClient invoiceServiceClient;

    @BeforeEach
    void setup() {
        this.invoiceServiceClient = Mockito.mock(InvoiceServiceClient.class);
        this.invoiceService = new InvoiceService(invoiceServiceClient);
    }

    @Test
    void getInvoice_invoiceIdGiven_passedToInvoiceService() {
        //given
        InvoiceResponse invoice = new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", "ISSUED", null, null, List.of(),
                null, null, null, "PLN", null, null);
        when(invoiceServiceClient.getInvoice(1L)).thenReturn(invoice);
        //when
        InvoiceResponse result = invoiceService.getInvoice(1L);
        //then
        Assertions.assertSame(invoice, result);
    }

    @Test
    void getInvoiceByOrder_orderIdGiven_passedToInvoiceService() {
        //given
        InvoiceResponse invoice = new InvoiceResponse(1L, "FV/2026/11/000001", 10L, "order-1", "ISSUED", null, null, List.of(),
                null, null, null, "PLN", null, null);
        when(invoiceServiceClient.getInvoiceByOrder(10L)).thenReturn(invoice);
        //when
        InvoiceResponse result = invoiceService.getInvoiceByOrder(10L);
        //then
        Assertions.assertEquals(10L, result.orderId());
    }

    @Test
    void getInvoices_emailAndPaginationGiven_passedToInvoiceService() {
        //given
        PageResponse<InvoiceSummaryResponse> page = new PageResponse<>(List.of(), 0, 20, 0L, 0);
        when(invoiceServiceClient.getInvoices(any(), any(), any(), any(), any())).thenReturn(page);
        //when
        PageResponse<InvoiceSummaryResponse> result = invoiceService.getInvoices("jan@example.com", new PaginationRequest(null, null, null, null));
        //then
        Mockito.verify(invoiceServiceClient).getInvoices("jan@example.com", null, null, null, null);
        Assertions.assertSame(page, result);
    }
}
