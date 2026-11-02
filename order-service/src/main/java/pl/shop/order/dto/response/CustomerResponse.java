package pl.shop.order.dto.response;

public record CustomerResponse(
        String firstName,
        String lastName,
        String email
) {
}
