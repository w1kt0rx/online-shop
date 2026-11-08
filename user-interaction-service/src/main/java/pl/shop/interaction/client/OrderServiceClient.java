package pl.shop.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import pl.shop.interaction.config.ServiceFeignConfig;
import pl.shop.interaction.dto.request.PlaceOrderRequest;
import pl.shop.interaction.dto.response.OrderResponse;
import pl.shop.interaction.dto.response.OrderSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;

@FeignClient(name = "order-service", configuration = ServiceFeignConfig.class)
public interface OrderServiceClient {

    @PostMapping("/orders")
    OrderResponse placeOrder(@RequestBody PlaceOrderRequest request);

    @GetMapping("/orders/{orderId}")
    OrderResponse getOrder(@PathVariable("orderId") Long orderId);

    @GetMapping("/orders")
    PageResponse<OrderSummaryResponse> getOrders(@RequestParam(value = "email", required = false) String email,
                                                 @RequestParam(value = "page", required = false) Integer page,
                                                 @RequestParam(value = "size", required = false) Integer size,
                                                 @RequestParam(value = "sortBy", required = false) String sortBy,
                                                 @RequestParam(value = "direction", required = false) String direction);

    @PostMapping("/orders/{orderId}/cancel")
    OrderResponse cancelOrder(@PathVariable("orderId") Long orderId);
}
