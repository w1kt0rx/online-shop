package pl.shop.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.product.dto.request.CreatePriceRequest;
import pl.shop.product.dto.response.PriceResponse;
import pl.shop.product.model.Price;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(target = "withNetPrice", ignore = true)
    @Mapping(target = "withVatRate", ignore = true)
    Price toEntity(CreatePriceRequest request);

    PriceResponse toDto(Price price);
}
