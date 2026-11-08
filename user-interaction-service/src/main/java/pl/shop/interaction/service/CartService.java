package pl.shop.interaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.shop.interaction.client.CartServiceClient;
import pl.shop.interaction.dto.request.AddCartItemRequest;
import pl.shop.interaction.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.interaction.dto.response.CartResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartServiceClient cartServiceClient;

    public CartResponse createCart() {
        log.debug("Creating cart");
        return cartServiceClient.createCart();
    }

    public CartResponse getCart(Long cartId) {
        log.debug("Fetching cart with id = {}", cartId);
        return cartServiceClient.getCart(cartId);
    }

    public CartResponse addItem(Long cartId, AddCartItemRequest request) {
        log.debug("Adding product with id = {} to cart with id = {}", request.productId(), cartId);
        return cartServiceClient.addItem(cartId, request);
    }

    public CartResponse changeItemQuantity(Long cartId, Long itemId, ChangeCartItemQuantityRequest request) {
        log.debug("Changing quantity of item with id = {} in cart with id = {}", itemId, cartId);
        return cartServiceClient.changeItemQuantity(cartId, itemId, request);
    }

    public CartResponse removeItem(Long cartId, Long itemId) {
        log.debug("Removing item with id = {} from cart with id = {}", itemId, cartId);
        return cartServiceClient.removeItem(cartId, itemId);
    }
}
