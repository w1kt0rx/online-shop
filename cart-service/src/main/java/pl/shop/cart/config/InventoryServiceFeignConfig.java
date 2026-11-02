package pl.shop.cart.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class InventoryServiceFeignConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new InventoryServiceErrorDecoder();
    }

    @Bean
    public Retryer retryer() {
        return new InventoryServiceRetryer(5, 100L);
    }
}
