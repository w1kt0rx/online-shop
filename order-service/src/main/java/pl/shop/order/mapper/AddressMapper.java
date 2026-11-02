package pl.shop.order.mapper;

import org.mapstruct.Mapper;
import pl.shop.order.dto.request.AddressRequest;
import pl.shop.order.dto.response.AddressResponse;
import pl.shop.order.model.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    Address toEntity(AddressRequest request);

    AddressResponse toAddressResponse(Address address);
}
