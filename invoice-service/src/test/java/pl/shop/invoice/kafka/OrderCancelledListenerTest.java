package pl.shop.invoice.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.invoice.kafka.event.OrderCancelledEvent;
import pl.shop.invoice.service.InvoiceService;

import java.time.LocalDateTime;

public class OrderCancelledListenerTest {
    private OrderCancelledListener listener;
    private InvoiceService invoiceService;

    @BeforeEach
    void setup() {
        this.invoiceService = Mockito.mock(InvoiceService.class);
        this.listener = new OrderCancelledListener(invoiceService);
    }

    @Test
    void onOrderCancelled_eventReceived_invoiceCancelled() {
        //given
        OrderCancelledEvent event = new OrderCancelledEvent(10L, "order-1", LocalDateTime.of(2026, 11, 3, 9, 0));
        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        //when
        listener.onOrderCancelled(event);
        //then
        Mockito.verify(invoiceService).cancelInvoice(eventCaptor.capture());
        Assertions.assertSame(event, eventCaptor.getValue());
    }
}
