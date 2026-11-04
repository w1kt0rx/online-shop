package pl.shop.invoice.kafka.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemData(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitNetPrice,
        BigDecimal vatRate,
        String currency,
        List<String> components
) {
}
