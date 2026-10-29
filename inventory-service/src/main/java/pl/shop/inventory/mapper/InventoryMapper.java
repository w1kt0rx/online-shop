package pl.shop.inventory.mapper;

import org.mapstruct.Mapper;

import pl.shop.inventory.dto.response.StockResponse;
import pl.shop.inventory.model.StockItem;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    StockResponse toResponse(StockItem stockItem);
}
