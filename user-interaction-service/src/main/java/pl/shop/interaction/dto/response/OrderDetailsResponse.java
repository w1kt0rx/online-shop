package pl.shop.interaction.dto.response;

public record OrderDetailsResponse(
        OrderResponse order,
        InvoiceResponse invoice
) {
}
