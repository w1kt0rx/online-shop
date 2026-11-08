package pl.shop.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pl.shop.interaction.config.ServiceFeignConfig;
import pl.shop.interaction.dto.request.AddCartItemRequest;
import pl.shop.interaction.dto.request.ChangeCartItemQuantityRequest;
import pl.shop.interaction.dto.response.CartResponse;

@FeignClient(name = "cart-service", configuration = ServiceFeignConfig.class)
public interface CartServiceClient {

    @PostMapping("/carts")
    CartResponse createCart();

    @GetMapping("/carts/{cartId}")
    CartResponse getCart(@PathVariable("cartId") Long cartId);

    @PostMapping("/carts/{cartId}/items")
    CartResponse addItem(@PathVariable("cartId") Long cartId, @RequestBody AddCartItemRequest request);

    @PatchMapping("/carts/{cartId}/items/{itemId}")
    CartResponse changeItemQuantity(@PathVariable("cartId") Long cartId, @PathVariable("itemId") Long itemId,
                                    @RequestBody ChangeCartItemQuantityRequest request);

    @DeleteMapping("/carts/{cartId}/items/{itemId}")
    CartResponse removeItem(@PathVariable("cartId") Long cartId, @PathVariable("itemId") Long itemId);
}
