package pl.shop.order.mapper;

import org.mapstruct.Mapper;
import pl.shop.order.dto.request.CustomerRequest;
import pl.shop.order.dto.response.CustomerResponse;
import pl.shop.order.model.Customer;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    Customer toEntity(CustomerRequest request);

    CustomerResponse toCustomerResponse(Customer customer);
}
