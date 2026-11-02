package pl.shop.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pl.shop.order.config.CartServiceFeignConfig;
import pl.shop.order.dto.response.CartResponse;

@FeignClient(name = "cart-service", configuration = CartServiceFeignConfig.class)
public interface CartServiceClient {
    @GetMapping("/carts/{cartId}")
    CartResponse getCart(@PathVariable("cartId") Long cartId);
}
