package pl.shop.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pl.shop.cart.config.ProductServiceFeignConfig;
import pl.shop.cart.dto.request.QuoteRequest;
import pl.shop.cart.dto.response.QuoteResponse;

@FeignClient(name = "product-service",
        configuration = ProductServiceFeignConfig.class)
public interface ProductServiceClient {

    @PostMapping("/products/{productId}/configurations/quote")
    QuoteResponse quote(@PathVariable("productId") Long productId, @RequestBody QuoteRequest request);
}
