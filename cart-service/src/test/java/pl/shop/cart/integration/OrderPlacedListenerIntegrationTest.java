package pl.shop.cart.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.wiremock.spring.EnableWireMock;
import pl.shop.cart.dto.request.AddCartItemRequest;
import pl.shop.cart.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.cart.kafka.OrderPlacedListener;
import pl.shop.cart.kafka.event.OrderPlacedEvent;
import pl.shop.cart.model.Cart;
import pl.shop.cart.model.CartItemComponent;
import pl.shop.cart.model.CartStatus;
import pl.shop.cart.model.Price;
import pl.shop.cart.repository.CartRepository;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
public class OrderPlacedListenerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private OrderPlacedListener orderPlacedListener;

    @AfterEach
    void tearDown() {
        WireMock.reset();
        cartRepository.deleteAll();
    }

    @Test
    void onOrderPlaced_activeCart_cartMarkedAsOrderedAndAddingItemRejected() throws Exception {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        OrderPlacedEvent event = new OrderPlacedEvent(1L, "3f6c2a1e-8b4d-4c1a-9e2f-7d5b0a9c1e42", cartId);
        AddCartItemRequest request = new AddCartItemRequest(1L, List.of(11L), 1);
        //when
        orderPlacedListener.onOrderPlaced(event);
        //then
        Assertions.assertEquals(CartStatus.ORDERED, cartRepository.findById(cartId).orElseThrow().getStatus());
        mockMvc.perform(post("/carts/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errorCode").value("CART_ALREADY_ORDERED"))
                .andExpect(jsonPath("$.message").value("Cart with id = " + cartId + " is already ordered"));
        mockMvc.perform(get("/carts/{cartId}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"));
        verify(0, anyRequestedFor(anyUrl()));
        transactionTemplate.executeWithoutResult(transaction ->
                Assertions.assertTrue(cartRepository.findById(cartId).orElseThrow().getItems().isEmpty()));
    }

    @Test
    void onOrderPlaced_cartWithItems_itemsKeptAndModificationsRejected() throws Exception {
        //given
        Cart cart = Cart.create();
        cart.addItem(1L, "Gaming PC Pro", new Price(new BigDecimal("5855.00"), new BigDecimal("0.23"), "PLN"), 2,
                List.of(new CartItemComponent(11L, 4L, "Graphics card", "RTX 4070")));
        Cart saved = cartRepository.save(cart);
        Long cartId = saved.getId();
        Long itemId = saved.getItems().getFirst().getId();
        OrderPlacedEvent event = new OrderPlacedEvent(2L, "9a1b7c3d-2e4f-4a6b-8c0d-1e2f3a4b5c6d", cartId);
        ChangeCartItemQuantityRequest request = new ChangeCartItemQuantityRequest(3);
        //when
        orderPlacedListener.onOrderPlaced(event);
        //then
        mockMvc.perform(patch("/carts/{cartId}/items/{itemId}", cartId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_ALREADY_ORDERED"));
        mockMvc.perform(delete("/carts/{cartId}/items/{itemId}", cartId, itemId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_ALREADY_ORDERED"));
        verify(0, anyRequestedFor(anyUrl()));
        transactionTemplate.executeWithoutResult(transaction -> {
            Cart reloaded = cartRepository.findById(cartId).orElseThrow();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(CartStatus.ORDERED, reloaded.getStatus()),
                    () -> Assertions.assertEquals(1, reloaded.getItems().size()),
                    () -> Assertions.assertEquals(itemId, reloaded.getItems().getFirst().getId()),
                    () -> Assertions.assertEquals(2, reloaded.getItems().getFirst().getQuantity()),
                    () -> Assertions.assertEquals(1, reloaded.getItems().getFirst().getComponents().size())
            );
        });
    }

    @Test
    void onOrderPlaced_eventDeliveredTwice_secondEventIgnoredAndCartStaysOrdered() {
        //given
        Long cartId = cartRepository.save(Cart.create()).getId();
        OrderPlacedEvent event = new OrderPlacedEvent(3L, "c4d5e6f7-a8b9-4c0d-9e1f-2a3b4c5d6e7f", cartId);
        orderPlacedListener.onOrderPlaced(event);
        //when //then
        Assertions.assertDoesNotThrow(() -> orderPlacedListener.onOrderPlaced(event));
        Assertions.assertAll(
                () -> Assertions.assertEquals(CartStatus.ORDERED, cartRepository.findById(cartId).orElseThrow().getStatus()),
                () -> Assertions.assertEquals(1, cartRepository.count())
        );
    }

    @Test
    void onOrderPlaced_cartNotExists_eventSkippedWithoutException() {
        //given
        Long activeCartId = cartRepository.save(Cart.create()).getId();
        OrderPlacedEvent event = new OrderPlacedEvent(4L, "d1e2f3a4-b5c6-4d7e-8f9a-0b1c2d3e4f5a", 999999L);
        //when //then
        Assertions.assertDoesNotThrow(() -> orderPlacedListener.onOrderPlaced(event));
        Assertions.assertAll(
                () -> Assertions.assertFalse(cartRepository.existsById(999999L)),
                () -> Assertions.assertEquals(1, cartRepository.count()),
                () -> Assertions.assertEquals(CartStatus.ACTIVE, cartRepository.findById(activeCartId).orElseThrow().getStatus())
        );
    }
}
