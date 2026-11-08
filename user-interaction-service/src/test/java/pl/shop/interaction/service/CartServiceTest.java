package pl.shop.interaction.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.interaction.client.CartServiceClient;
import pl.shop.interaction.dto.request.AddCartItemRequest;
import pl.shop.interaction.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.interaction.dto.response.CartResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class CartServiceTest {
    private CartService cartService;
    private CartServiceClient cartServiceClient;

    @BeforeEach
    void setup() {
        this.cartServiceClient = Mockito.mock(CartServiceClient.class);
        this.cartService = new CartService(cartServiceClient);
    }

    @Test
    void createCart_noArguments_cartFromCartServiceReturned() {
        //given
        CartResponse cart = new CartResponse(1L, "ACTIVE", List.of(), null);
        when(cartServiceClient.createCart()).thenReturn(cart);
        //when
        CartResponse result = cartService.createCart();
        //then
        Assertions.assertSame(cart, result);
    }

    @Test
    void getCart_cartIdGiven_passedToCartService() {
        //given
        CartResponse cart = new CartResponse(1L, "ACTIVE", List.of(), null);
        when(cartServiceClient.getCart(1L)).thenReturn(cart);
        //when
        CartResponse result = cartService.getCart(1L);
        //then
        Assertions.assertSame(cart, result);
    }

    @Test
    void addItem_requestGiven_passedToCartService() {
        //given
        AddCartItemRequest request = new AddCartItemRequest(5L, List.of(2L, 4L), 1);
        ArgumentCaptor<AddCartItemRequest> requestCaptor = ArgumentCaptor.forClass(AddCartItemRequest.class);
        when(cartServiceClient.addItem(anyLong(), any(AddCartItemRequest.class))).thenReturn(new CartResponse(1L, "ACTIVE", List.of(), null));
        //when
        cartService.addItem(1L, request);
        //then
        Mockito.verify(cartServiceClient).addItem(Mockito.eq(1L), requestCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(5L, requestCaptor.getValue().productId()),
                () -> Assertions.assertEquals(List.of(2L, 4L), requestCaptor.getValue().configurationIds()),
                () -> Assertions.assertEquals(1, requestCaptor.getValue().quantity())
        );
    }

    @Test
    void changeItemQuantity_requestGiven_passedToCartService() {
        //given
        ArgumentCaptor<ChangeCartItemQuantityRequest> requestCaptor = ArgumentCaptor.forClass(ChangeCartItemQuantityRequest.class);
        when(cartServiceClient.changeItemQuantity(anyLong(), anyLong(), any(ChangeCartItemQuantityRequest.class)))
                .thenReturn(new CartResponse(1L, "ACTIVE", List.of(), null));
        //when
        cartService.changeItemQuantity(1L, 100L, new ChangeCartItemQuantityRequest(3));
        //then
        Mockito.verify(cartServiceClient).changeItemQuantity(Mockito.eq(1L), Mockito.eq(100L), requestCaptor.capture());
        Assertions.assertEquals(3, requestCaptor.getValue().quantity());
    }

    @Test
    void removeItem_idsGiven_passedToCartService() {
        //given
        when(cartServiceClient.removeItem(1L, 100L)).thenReturn(new CartResponse(1L, "ACTIVE", List.of(), null));
        //when
        CartResponse result = cartService.removeItem(1L, 100L);
        //then
        Mockito.verify(cartServiceClient).removeItem(1L, 100L);
        Assertions.assertTrue(result.items().isEmpty());
    }
}
