package pl.shop.interaction.dto.response;

public record CustomerResponse(
        String firstName,
        String lastName,
        String email
) {
}
