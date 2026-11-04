package pl.shop.invoice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.service.InvoiceService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPlacedListener {
    private final InvoiceService invoiceService;

    @KafkaListener(topics = "${app.kafka.topics.order-placed}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlaced event, orderNumber = {}", event.orderNumber());
        invoiceService.createInvoice(event);
    }
}
