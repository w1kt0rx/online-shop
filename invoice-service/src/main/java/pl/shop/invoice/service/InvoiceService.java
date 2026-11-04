package pl.shop.invoice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.shop.invoice.dto.request.PaginationRequest;
import pl.shop.invoice.dto.response.InvoiceResponse;
import pl.shop.invoice.dto.response.InvoiceSummaryResponse;
import pl.shop.invoice.dto.response.PageResponse;
import pl.shop.invoice.exception.InvoiceNotFoundException;
import pl.shop.invoice.kafka.event.OrderCancelledEvent;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.mapper.InvoiceMapper;
import pl.shop.invoice.model.Invoice;
import pl.shop.invoice.repository.InvoiceRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Transactional
    public void createInvoice(OrderPlacedEvent event) {
        log.debug("Attempting to create invoice for order with id = {}", event.orderId());
        if (invoiceRepository.existsByOrderId(event.orderId())) {
            log.warn("Invoice for order with id = {} already exists, skipping", event.orderId());
            return;
        }
        Invoice invoice = issueInvoice(event);
        event.items().forEach(item -> addLine(invoice, item));
        invoiceRepository.save(invoice);
        log.info("Invoice {} created for order {}", invoice.getInvoiceNumber(), event.orderNumber());
    }

    @Transactional
    public void cancelInvoice(OrderCancelledEvent event) {
        log.debug("Attempting to cancel invoice for order with id = {}", event.orderId());
        Invoice invoice = invoiceRepository.findByOrderId(event.orderId())
                .orElseThrow(() -> InvoiceNotFoundException.forOrder(event.orderId()));
        if (invoice.isCancelled()) {
            log.warn("Invoice {} is already cancelled, skipping", invoice.getInvoiceNumber());
            return;
        }
        invoice.cancel(event.cancelledAt());
        log.info("Invoice {} cancelled for order {}", invoice.getInvoiceNumber(), event.orderNumber());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId) {
        log.debug("Attempting to fetch invoice with id = {}", invoiceId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> InvoiceNotFoundException.byId(invoiceId));
        log.info("Invoice with id = {} fetched", invoiceId);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrder(Long orderId) {
        log.debug("Attempting to fetch invoice for order with id = {}", orderId);
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> InvoiceNotFoundException.forOrder(orderId));
        log.info("Invoice for order with id = {} fetched", orderId);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> getInvoices(String email, PaginationRequest paginationRequest) {
        log.debug("Attempting to fetch invoices, email = {}", email);
        Page<Invoice> invoices = email == null || email.isBlank()
                ? invoiceRepository.findAll(paginationRequest.toPageable())
                : invoiceRepository.findByBuyerEmailIgnoreCase(email, paginationRequest.toPageable());
        log.info("Fetched {} invoices", invoices.getNumberOfElements());
        return PageResponse.from(invoices.map(invoiceMapper::toSummaryResponse));
    }

    private Invoice issueInvoice(OrderPlacedEvent event) {
        LocalDateTime issuedAt = LocalDateTime.now();
        return Invoice.issue(
                invoiceNumberGenerator.next(issuedAt),
                event.orderId(),
                event.orderNumber(),
                invoiceMapper.toBuyer(event.customer()),
                invoiceMapper.toAddress(event.shippingAddress()),
                event.currency(),
                issuedAt
        );
    }

    private void addLine(Invoice invoice, OrderPlacedEvent.OrderItemData item) {
        invoice.addLine(
                item.productName(),
                describe(item.components()),
                item.quantity(),
                item.unitNetPrice(),
                item.vatRate()
        );
    }

    private static String describe(List<String> components) {
        return components == null || components.isEmpty() ? null : String.join(", ", components);
    }
}
