package pl.shop.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.order.dto.response.PriceResponse;
import pl.shop.order.model.Price;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(target = "multiply", ignore = true)
    Price toEntity(PriceResponse response);

    PriceResponse toResponse(Price price);
}
