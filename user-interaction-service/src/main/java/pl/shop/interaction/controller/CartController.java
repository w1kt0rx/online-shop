package pl.shop.interaction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.interaction.dto.request.AddCartItemRequest;
import pl.shop.interaction.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.interaction.dto.response.CartResponse;
import pl.shop.interaction.service.CartService;

@Slf4j
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CartResponse createCart() {
        log.info("Received POST request to create cart");
        return cartService.createCart();
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{cartId}")
    public CartResponse getCart(@PathVariable Long cartId) {
        log.info("Received GET request to fetch cart with id = {}", cartId);
        return cartService.getCart(cartId);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{cartId}/items")
    public CartResponse addItem(@PathVariable Long cartId, @RequestBody AddCartItemRequest request) {
        log.info("Received POST request to add product with id = {} to cart with id = {}", request.productId(), cartId);
        return cartService.addItem(cartId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{cartId}/items/{itemId}")
    public CartResponse changeItemQuantity(@PathVariable Long cartId, @PathVariable Long itemId,
                                           @RequestBody ChangeCartItemQuantityRequest request) {
        log.info("Received PATCH request to change quantity of item with id = {} in cart with id = {}", itemId, cartId);
        return cartService.changeItemQuantity(cartId, itemId, request);
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{cartId}/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long cartId, @PathVariable Long itemId) {
        log.info("Received DELETE request to remove item with id = {} from cart with id = {}", itemId, cartId);
        return cartService.removeItem(cartId, itemId);
    }
}
