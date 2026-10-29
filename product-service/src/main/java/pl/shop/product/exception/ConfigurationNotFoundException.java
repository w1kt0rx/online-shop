package pl.shop.product.exception;

import org.springframework.http.HttpStatus;

public class ConfigurationNotFoundException extends ProductServiceException {
    public ConfigurationNotFoundException(Long configurationId) {
        super("CONFIGURATION_NOT_FOUND", "Configuration not found: id = " + configurationId, HttpStatus.NOT_FOUND);
    }
}
