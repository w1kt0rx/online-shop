package pl.shop.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.shop.cart.config.InventoryServiceFeignConfig;
import pl.shop.cart.dto.response.StockResponse;

import java.util.List;

@FeignClient(name = "inventory-service",
        configuration = InventoryServiceFeignConfig.class)
public interface InventoryServiceClient {

    @GetMapping("/inventory")
    List<StockResponse> getStock(@RequestParam("productIds") List<Long> productIds);
}
