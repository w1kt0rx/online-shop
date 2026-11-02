package pl.shop.order.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class CartServiceFeignConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CartServiceErrorDecoder();
    }

    @Bean
    public Retryer retryer() {
        return new CartServiceRetryer(5, 100L);
    }
}
