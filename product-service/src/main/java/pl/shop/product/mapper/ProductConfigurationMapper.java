package pl.shop.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.shop.product.dto.response.ProductConfigurationResponse;
import pl.shop.product.dto.response.QuoteComponentResponse;
import pl.shop.product.dto.response.QuoteResponse;
import pl.shop.product.model.Product;
import pl.shop.product.model.ProductConfiguration;
import pl.shop.product.model.Quote;

@Mapper(componentModel = "spring", uses = PriceMapper.class)
public interface ProductConfigurationMapper {

    @Mapping(target = "componentId", source = "component.id")
    @Mapping(target = "componentName", source = "component.name")
    ProductConfigurationResponse toResponse(ProductConfiguration configuration);

    @Mapping(target = "configurationId", source = "id")
    @Mapping(target = "componentProductId", source = "component.id")
    @Mapping(target = "componentName", source = "component.name")
    QuoteComponentResponse toQuoteComponentResponse(ProductConfiguration configuration);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "selectedComponents", source = "quote.selectedConfigurations")
    @Mapping(target = "unitPrice", source = "quote.unitPrice")
    @Mapping(target = "stockProductIds", source = "quote.stockProductIds")
    QuoteResponse toQuoteResponse(Product product, Quote quote);
}
