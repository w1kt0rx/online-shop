package pl.shop.interaction.dto.request;

public record PaginationRequest(
        Integer page,
        Integer size,
        String sortBy,
        String direction
) {
}
