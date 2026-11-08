package pl.shop.interaction.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.shop.interaction.dto.request.PaginationRequest;
import pl.shop.interaction.dto.request.PlaceOrderRequest;
import pl.shop.interaction.dto.response.OrderDetailsResponse;
import pl.shop.interaction.dto.response.OrderResponse;
import pl.shop.interaction.dto.response.OrderSummaryResponse;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.service.OrderService;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public OrderResponse placeOrder(@RequestBody PlaceOrderRequest request) {
        log.info("Received POST request to place order for cart with id = {}", request.cartId());
        return orderService.placeOrder(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{orderId}")
    public OrderDetailsResponse getOrder(@PathVariable Long orderId) {
        log.info("Received GET request to fetch order with id = {}", orderId);
        return orderService.getOrderDetails(orderId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResponse<OrderSummaryResponse> getOrders(@RequestParam(required = false) String email,
                                                        @ModelAttribute PaginationRequest pagination) {
        log.info("Received GET request to fetch orders, email = {}", email);
        return orderService.getOrders(email, pagination);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId) {
        log.info("Received POST request to cancel order with id = {}", orderId);
        return orderService.cancelOrder(orderId);
    }
}
