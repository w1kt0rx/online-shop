package pl.shop.order.controller;

import jakarta.validation.Valid;
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
import pl.shop.order.dto.request.PaginationRequest;
import pl.shop.order.dto.request.PlaceOrderRequest;
import pl.shop.order.dto.response.OrderResponse;
import pl.shop.order.dto.response.OrderSummaryResponse;
import pl.shop.order.dto.response.PageResponse;
import pl.shop.order.service.OrderService;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        log.info("Received POST request to place order for cart with id = {}", request.cartId());
        return orderService.placeOrder(request);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        log.info("Received GET request to fetch order with id = {}", orderId);
        return orderService.getOrder(orderId);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResponse<OrderSummaryResponse> getOrders(@RequestParam(required = false) String email,
                                                        @Valid @ModelAttribute PaginationRequest paginationRequest) {
        log.info("Received GET request to fetch orders, email = {}", email);
        return orderService.getOrders(email, paginationRequest);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId) {
        log.info("Received POST request to cancel order with id = {}", orderId);
        return orderService.cancelOrder(orderId);
    }
}
