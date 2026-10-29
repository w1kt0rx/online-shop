package pl.shop.product.dto.response;

public record ProductConfigurationResponse(
        Long id,
        String slot,
        Long componentId,
        String componentName,
        boolean defaultComponent,
        boolean multiSelect
) {}