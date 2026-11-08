package pl.shop.interaction.dto.response;

public record ProductConfigurationResponse(
        Long id,
        String slot,
        Long componentId,
        String componentName,
        boolean defaultComponent,
        boolean multiSelect
) {
}
