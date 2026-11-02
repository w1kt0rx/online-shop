package pl.shop.product.model;

import java.util.List;

public record Quote(
        List<ProductConfiguration> selectedConfigurations,
        Price unitPrice,
        List<Long> stockProductIds
) {
}
