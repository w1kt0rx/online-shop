package pl.shop.invoice.dto.response;

public record BuyerResponse(
        String firstName,
        String lastName,
        String email
) {
}
