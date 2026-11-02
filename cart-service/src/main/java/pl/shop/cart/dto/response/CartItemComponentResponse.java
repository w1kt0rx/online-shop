package pl.shop.cart.dto.response;

public record CartItemComponentResponse(
        Long configurationId,
        Long componentProductId,
        String slot,
        String componentName
) {
}
