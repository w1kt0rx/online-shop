package pl.shop.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pl.shop.interaction.config.ServiceFeignConfig;
import pl.shop.interaction.dto.response.InvoiceResponse;
import pl.shop.interaction.dto.response.InvoiceSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

@FeignClient(name = "invoice-service", configuration = ServiceFeignConfig.class)
public interface InvoiceServiceClient {

    @GetMapping("/invoices/{invoiceId}")
    InvoiceResponse getInvoice(@PathVariable("invoiceId") Long invoiceId);

    @GetMapping("/invoices/orders/{orderId}")
    InvoiceResponse getInvoiceByOrder(@PathVariable("orderId") Long orderId);

    @GetMapping("/invoices")
    PageResponse<InvoiceSummaryResponse> getInvoices(@RequestParam(value = "email", required = false) String email,
                                                     @RequestParam(value = "page", required = false) Integer page,
                                                     @RequestParam(value = "size", required = false) Integer size,
                                                     @RequestParam(value = "sortBy", required = false) String sortBy,
                                                     @RequestParam(value = "direction", required = false) String direction);
}
