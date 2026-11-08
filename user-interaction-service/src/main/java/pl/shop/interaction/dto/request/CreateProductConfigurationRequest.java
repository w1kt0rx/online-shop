package pl.shop.interaction.dto.request;

public record CreateProductConfigurationRequest(
        Long componentId,
        String slot,
        boolean defaultComponent,
        boolean multiSelect
) {
}
