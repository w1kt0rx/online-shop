package pl.shop.order.dto.response;

public record OrderItemComponentResponse(
        Long configurationId,
        Long componentProductId,
        String slot,
        String componentName
) {
}
