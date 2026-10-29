package pl.shop.product.dto.response;

import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String category,
        String vendor,
        PriceResponse price,
        boolean sellableSeparately,
        List<ProductConfigurationResponse> configurations


) {
}
