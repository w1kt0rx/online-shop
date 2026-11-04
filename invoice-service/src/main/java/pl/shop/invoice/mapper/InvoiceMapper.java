package pl.shop.invoice.mapper;

import org.mapstruct.Mapper;
import pl.shop.invoice.dto.response.AddressResponse;
import pl.shop.invoice.dto.response.BuyerResponse;
import pl.shop.invoice.dto.response.InvoiceLineResponse;
import pl.shop.invoice.dto.response.InvoiceResponse;
import pl.shop.invoice.dto.response.InvoiceSummaryResponse;
import pl.shop.invoice.kafka.event.OrderPlacedEvent;
import pl.shop.invoice.model.Address;
import pl.shop.invoice.model.Buyer;
import pl.shop.invoice.model.Invoice;
import pl.shop.invoice.model.InvoiceLine;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    InvoiceResponse toResponse(Invoice invoice);

    InvoiceSummaryResponse toSummaryResponse(Invoice invoice);

    InvoiceLineResponse toLineResponse(InvoiceLine line);

    BuyerResponse toBuyerResponse(Buyer buyer);

    AddressResponse toAddressResponse(Address address);

    Buyer toBuyer(OrderPlacedEvent.CustomerData customer);

    Address toAddress(OrderPlacedEvent.AddressData address);
}
