package pl.shop.order.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.order.exception.InvalidQuantityException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false)
    private String productName;
    @Embedded
    private Price unitPrice;
    @Column(nullable = false)
    private int quantity;
    @ElementCollection
    @CollectionTable(name = "order_item_components", joinColumns = @JoinColumn(name = "order_item_id"))
    private List<OrderItemComponent> components = new ArrayList<>();

    OrderItem(Order order, Long productId, String productName, Price unitPrice, int quantity, List<OrderItemComponent> components) {
        validateQuantity(quantity);
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.components = new ArrayList<>(components);
    }

    public Price getTotalPrice() {
        return unitPrice.multiply(quantity);
    }

    public List<Long> getStockProductIds() {
        return Stream.concat(
                Stream.of(productId),
                components.stream().map(
                        OrderItemComponent::getComponentProductId
                )
        ).toList();
    }

    public List<OrderItemComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidQuantityException(quantity);
        }
    }
}