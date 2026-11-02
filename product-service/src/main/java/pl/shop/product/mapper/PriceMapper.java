package pl.shop.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.product.dto.request.PriceRequest;
import pl.shop.product.dto.response.PriceResponse;
import pl.shop.product.model.Price;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(target = "withNetPrice", ignore = true)
    Price toEntity(PriceRequest request);

    PriceResponse toResponse(Price price);
}
