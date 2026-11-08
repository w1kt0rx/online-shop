package pl.shop.interaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.shop.interaction.client.InvoiceServiceClient;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.InvoiceSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceServiceClient invoiceServiceClient;

    public InvoiceResponse getInvoice(Long invoiceId) {
        log.debug("Fetching invoice with id = {}", invoiceId);
        return invoiceServiceClient.getInvoice(invoiceId);
    }

    public InvoiceResponse getInvoiceByOrder(Long orderId) {
        log.debug("Fetching invoice for order with id = {}", orderId);
        return invoiceServiceClient.getInvoiceByOrder(orderId);
    }

    public PageResponse<InvoiceSummaryResponse> getInvoices(String email, PaginationRequest pagination) {
        log.debug("Fetching invoices, email = {}", email);
        return invoiceServiceClient.getInvoices(email, pagination.page(), pagination.size(),
                pagination.sortBy(), pagination.direction());
    }
}
