package pl.shop.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import pl.shop.interaction.config.ServiceFeignConfig;
import pl.shop.interaction.dto.request.CreateProductConfigurationRequest;
import pl.shop.interaction.dto.request.CreateProductRequest;
import pl.shop.interaction.dto.request.QuoteRequest;
import pl.shop.interaction.dto.request.UpdateProductRequest;
import pl.shop.interaction.dto.response.PageResponse;
import pl.shop.interaction.dto.response.ProductConfigurationResponse;
import pl.shop.interaction.dto.response.ProductResponse;
import pl.shop.interaction.dto.response.ProductSummaryResponse;
import pl.shop.interaction.dto.response.QuoteResponse;

import java.util.List;

@FeignClient(name = "product-service", configuration = ServiceFeignConfig.class)
public interface ProductServiceClient {

    @GetMapping("/products")
    PageResponse<ProductSummaryResponse> getProducts(@RequestParam(value = "category", required = false) String category,
                                                     @RequestParam(value = "page", required = false) Integer page,
                                                     @RequestParam(value = "size", required = false) Integer size,
                                                     @RequestParam(value = "sortBy", required = false) String sortBy,
                                                     @RequestParam(value = "direction", required = false) String direction);

    @GetMapping("/products/{productId}")
    ProductResponse getProduct(@PathVariable("productId") Long productId);

    @PostMapping("/products")
    ProductResponse createProduct(@RequestBody CreateProductRequest request);

    @PutMapping("/products/{productId}")
    ProductResponse updateProduct(@PathVariable("productId") Long productId, @RequestBody UpdateProductRequest request);

    @DeleteMapping("/products/{productId}")
    void deleteProduct(@PathVariable("productId") Long productId);

    @GetMapping("/products/{productId}/configurations")
    List<ProductConfigurationResponse> getConfigurations(@PathVariable("productId") Long productId);

    @PostMapping("/products/{productId}/configurations")
    ProductConfigurationResponse createConfiguration(@PathVariable("productId") Long productId,
                                                     @RequestBody CreateProductConfigurationRequest request);

    @DeleteMapping("/products/{productId}/configurations/{configurationId}")
    void deleteConfiguration(@PathVariable("productId") Long productId, @PathVariable("configurationId") Long configurationId);

    @PostMapping("/products/{productId}/configurations/quote")
    QuoteResponse quote(@PathVariable("productId") Long productId, @RequestBody QuoteRequest request);
}
