package pl.shop.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pl.shop.order.config.InventoryServiceFeignConfig;
import pl.shop.order.dto.request.StockReservationRequest;
import pl.shop.order.dto.response.StockResponse;

import java.util.List;

@FeignClient(name = "inventory-service", configuration = InventoryServiceFeignConfig.class)
public interface InventoryServiceClient {
    @PostMapping("/inventory/reservations")
    List<StockResponse> reserve(@RequestBody StockReservationRequest request);

    @PostMapping("/inventory/releases")
    List<StockResponse> release(@RequestBody StockReservationRequest request);
}