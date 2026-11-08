package pl.shop.interaction.config;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

public class ServiceErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 503) {
            FeignException exception = FeignException.errorStatus(methodKey, response);
            return new RetryableException(
                    response.status(),
                    exception.getMessage(),
                    response.request().httpMethod(),
                    exception,
                    100L,
                    response.request()
            );
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
