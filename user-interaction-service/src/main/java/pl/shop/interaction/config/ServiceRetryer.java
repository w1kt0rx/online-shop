package pl.shop.interaction.config;

import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceRetryer implements Retryer {
    private final int retryMaxAttempt;
    private final long retryInterval;
    private int attempt = 1;

    public ServiceRetryer(int retryMaxAttempt, long retryInterval) {
        this.retryMaxAttempt = retryMaxAttempt;
        this.retryInterval = retryInterval;
    }

    @Override
    public void continueOrPropagate(RetryableException e) {
        log.info("Feign retry attempt {} due to {}", attempt, e.getMessage());
        if (attempt >= retryMaxAttempt) {
            throw e;
        }
        attempt++;
        try {
            Thread.sleep(retryInterval);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public Retryer clone() {
        return new ServiceRetryer(retryMaxAttempt, retryInterval);
    }
}
