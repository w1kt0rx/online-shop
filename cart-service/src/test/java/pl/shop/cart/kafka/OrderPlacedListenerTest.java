package pl.shop.cart.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.cart.kafka.event.OrderPlacedEvent;
import pl.shop.cart.service.CartService;

public class OrderPlacedListenerTest {
    private OrderPlacedListener listener;
    private CartService cartService;

    @BeforeEach
    void setup() {
        this.cartService = Mockito.mock(CartService.class);
        this.listener = new OrderPlacedListener(cartService);
    }

    @Test
    void onOrderPlaced_eventReceived_cartMarkedAsOrdered() {
        //given
        OrderPlacedEvent event = new OrderPlacedEvent(1L, "3f6c2a1e-8b4d-4c1a-9e2f-7d5b0a9c1e42", 7L);
        ArgumentCaptor<Long> cartIdCaptor = ArgumentCaptor.forClass(Long.class);
        //when
        listener.onOrderPlaced(event);
        //then
        Mockito.verify(cartService).markAsOrdered(cartIdCaptor.capture());
        Assertions.assertEquals(7L, cartIdCaptor.getValue());
    }
}
