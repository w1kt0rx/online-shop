package pl.shop.cart.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class ProductServiceFeignConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ProductServiceErrorDecoder();
    }

    @Bean
    public Retryer retryer() {
        return new ProductServiceRetryer(5, 100L);
    }
}
