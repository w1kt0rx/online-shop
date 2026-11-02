package pl.shop.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.cart.dto.response.PriceResponse;
import pl.shop.cart.model.Price;

@Mapper(componentModel = "spring")
public interface PriceMapper {

    @Mapping(target = "multiply", ignore = true)
    Price toEntity(PriceResponse response);

    PriceResponse toResponse(Price price);
}
