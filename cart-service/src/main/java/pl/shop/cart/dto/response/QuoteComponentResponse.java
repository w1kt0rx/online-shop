package pl.shop.cart.dto.response;

public record QuoteComponentResponse(
        Long configurationId,
        Long componentProductId,
        String slot,
        String componentName
) {
}
