package pl.shop.inventory.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.inventory.dto.response.StockResponse;
import pl.shop.inventory.model.StockItem;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "available", source = "inStock")
    StockResponse toResponse(StockItem stockItem);
}
