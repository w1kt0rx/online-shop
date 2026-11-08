package pl.shop.interaction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.InvoiceSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.service.InvoiceService;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable Long invoiceId) {
        log.info("Received GET request to fetch invoice with id = {}", invoiceId);
        return invoiceService.getInvoice(invoiceId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/orders/{orderId}")
    public InvoiceResponse getInvoiceByOrder(@PathVariable Long orderId) {
        log.info("Received GET request to fetch invoice for order with id = {}", orderId);
        return invoiceService.getInvoiceByOrder(orderId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResponse<InvoiceSummaryResponse> getInvoices(@RequestParam(required = false) String email,
                                                            @ModelAttribute PaginationRequest pagination) {
        log.info("Received GET request to fetch invoices, email = {}", email);
        return invoiceService.getInvoices(email, pagination);
    }
}
