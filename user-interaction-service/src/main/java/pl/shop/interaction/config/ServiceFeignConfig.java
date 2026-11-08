package pl.shop.interaction.config;

import feign.Client;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;

public class ServiceFeignConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ServiceErrorDecoder();
    }

    @Bean
    public Retryer retryer() {
        return new ServiceRetryer(5, 100L);
    }

    @Bean
    public Client feignClient() {
        return new ApacheHttp5Client(HttpClients.custom()
                .disableAutomaticRetries()
                .build());
    }
}
