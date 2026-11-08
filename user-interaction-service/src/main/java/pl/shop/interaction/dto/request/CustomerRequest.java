package pl.shop.interaction.dto.request;

public record CustomerRequest(
        String firstName,
        String lastName,
        String email
) {
}
