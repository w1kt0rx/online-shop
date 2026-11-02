package pl.shop.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.order.dto.response.*;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItem;
import pl.shop.order.model.OrderItemComponent;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {PriceMapper.class, CustomerMapper.class, AddressMapper.class})
public interface OrderMapper {
    @Mapping(target = "total", source = ".")
    OrderResponse toResponse(Order order);

    @Mapping(target = "netPrice", source = "totalNet")
    @Mapping(target = "vatAmount", source = "totalVat")
    @Mapping(target = "grossPrice", source = "totalGross")
    @Mapping(target = "currency", constant = "PLN")
    OrderTotalResponse toTotalResponse(Order order);

    @Mapping(target = "grossPrice", source = "totalGross")
    @Mapping(target = "currency", constant = "PLN")
    OrderSummaryResponse toSummaryResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    OrderItemComponentResponse toComponentResponse(OrderItemComponent component);

    OrderItemComponent toComponent(CartItemComponentResponse response);

    List<OrderItemComponent> toComponents(List<CartItemComponentResponse> responses);
}
