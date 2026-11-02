package pl.shop.order.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderPlacedEvent(
        Long orderId,
        String orderNumber,
        Long cartId,
        LocalDateTime placedAt,
        CustomerData customer,
        AddressData shippingAddress,
        List<OrderItemData> items,
        BigDecimal totalNet,
        BigDecimal totalVat,
        BigDecimal totalGross,
        String currency
) {
    public record CustomerData(String firstName, String lastName, String email) {
    }

    public record AddressData(String street, String city, String postalCode, String country) {
    }

    public record OrderItemData(Long productId, String productName, int quantity,
                                BigDecimal unitNetPrice, BigDecimal vatRate, String currency,
                                List<String> components) {
    }
}
