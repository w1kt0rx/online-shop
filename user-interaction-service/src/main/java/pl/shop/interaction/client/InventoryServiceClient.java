package pl.shop.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import pl.shop.interaction.config.ServiceFeignConfig;
import pl.shop.interaction.dto.request.ChangeQuantityRequest;
import pl.shop.interaction.dto.request.DeliveryRequest;
import pl.shop.interaction.dto.response.StockResponse;

import java.util.List;

@FeignClient(name = "inventory-service", configuration = ServiceFeignConfig.class)
public interface InventoryServiceClient {

    @GetMapping("/inventory/{productId}")
    StockResponse getStock(@PathVariable("productId") Long productId);

    @GetMapping("/inventory")
    List<StockResponse> getStocks(@RequestParam("productIds") List<Long> productIds);

    @PutMapping("/inventory/{productId}")
    StockResponse changeQuantity(@PathVariable("productId") Long productId, @RequestBody ChangeQuantityRequest request);

    @PostMapping("/inventory/{productId}/deliveries")
    StockResponse registerDelivery(@PathVariable("productId") Long productId, @RequestBody DeliveryRequest request);
}
