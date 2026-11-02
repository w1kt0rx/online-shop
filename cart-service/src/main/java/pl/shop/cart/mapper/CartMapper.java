package pl.shop.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.cart.dto.response.CartItemComponentResponse;
import pl.shop.cart.dto.response.CartItemResponse;
import pl.shop.cart.dto.response.CartResponse;
import pl.shop.cart.dto.response.CartTotalResponse;
import pl.shop.cart.dto.response.QuoteComponentResponse;
import pl.shop.cart.model.Cart;
import pl.shop.cart.model.CartItem;
import pl.shop.cart.model.CartItemComponent;

import java.util.List;

@Mapper(componentModel = "spring", uses = PriceMapper.class)
public interface CartMapper {

    @Mapping(target = "total", source = ".")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "netPrice", source = "totalNet")
    @Mapping(target = "vatAmount", source = "totalVat")
    @Mapping(target = "grossPrice", source = "totalGross")
    @Mapping(target = "currency", constant = "PLN")
    CartTotalResponse toTotalResponse(Cart cart);

    CartItemResponse toItemResponse(CartItem item);

    CartItemComponentResponse toComponentResponse(CartItemComponent component);

    CartItemComponent toComponent(QuoteComponentResponse response);

    List<CartItemComponent> toComponents(List<QuoteComponentResponse> responses);
}
