package pl.shop.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.model.ProductConfiguration;


@Mapper(componentModel = "spring")
public interface ProductConfigurationMapper {

    @Mapping(target = "componentId", source = "component.id")
    @Mapping(target = "componentName", source = "component.name")
    ProductConfigurationResponse toDto(ProductConfiguration configuration);
}
