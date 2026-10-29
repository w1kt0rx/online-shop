package pl.shop.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.product.dto.request.CreateProductRequest;
import pl.shop.product.dto.response.ProductResponse;
import pl.shop.product.dto.response.ProductSummaryResponse;
import pl.shop.product.model.Product;

@Mapper(componentModel = "spring", uses = {PriceMapper.class, ProductConfigurationMapper.class})
public interface ProductMapper {
    @Mapping(target = "configurations", ignore = true)
    Product toEntity(CreateProductRequest request);

    ProductResponse toResponse(Product product);

    ProductSummaryResponse toSummaryResponse(Product product);
}
