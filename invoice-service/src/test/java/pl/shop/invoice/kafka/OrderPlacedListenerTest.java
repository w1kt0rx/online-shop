package pl.shop.invoice.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.service.InvoiceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderPlacedListenerTest {
    private OrderPlacedListener listener;
    private InvoiceService invoiceService;

    @BeforeEach
    void setup() {
        this.invoiceService = Mockito.mock(InvoiceService.class);
        this.listener = new OrderPlacedListener(invoiceService);
    }

    @Test
    void onOrderPlaced_eventReceived_invoiceCreatedFromEvent() {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(10L, "order-1", LocalDateTime.of(2026, 11, 2, 12, 0),
                new OrderPlacedEvent.CustomerData("Jan", "Kowalski", "jan@example.com"),
                new OrderPlacedEvent.AddressData("Długa 5/3", "Gdańsk", "80-001", "PL"),
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PLN");
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        //when
        listener.onOrderPlaced(event);
        //then
        Mockito.verify(invoiceService).createInvoice(eventCaptor.capture());
        Assertions.assertSame(event, eventCaptor.getValue());
    }
}
