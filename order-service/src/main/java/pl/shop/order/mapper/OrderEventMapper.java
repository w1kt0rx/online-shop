package pl.shop.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.order.kafka.event.OrderCancelledEvent;
import pl.shop.order.kafka.event.OrderPlacedEvent;
import pl.shop.order.model.Order;
import pl.shop.order.model.OrderItem;
import pl.shop.order.model.OrderItemComponent;

@Mapper(componentModel = "spring")
public interface OrderEventMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "placedAt", source = "createdAt")
    @Mapping(target = "currency", constant = "PLN")
    OrderPlacedEvent toOrderPlacedEvent(Order order);

    @Mapping(target = "unitNetPrice", source = "unitPrice.netPrice")
    @Mapping(target = "vatRate", source = "unitPrice.vatRate")
    @Mapping(target = "currency", source = "unitPrice.currency")
    OrderPlacedEvent.OrderItemData toItemData(OrderItem item);

    @Mapping(target = "orderId", source = "id")
    OrderCancelledEvent toOrderCancelledEvent(Order order);

    default String toComponentDescription(OrderItemComponent component) {
        return component.getSlot() + ": " + component.getComponentName();
    }
}
