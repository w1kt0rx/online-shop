package pl.shop.invoice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.shop.invoice.kafka.event.OrderCancelledEvent;
import pl.shop.invoice.service.InvoiceService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledListener {
    private final InvoiceService invoiceService;

    @KafkaListener(topics = "${app.kafka.topics.order-cancelled}")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelled event, orderNumber = {}", event.orderNumber());
        invoiceService.cancelInvoice(event);
    }
}
