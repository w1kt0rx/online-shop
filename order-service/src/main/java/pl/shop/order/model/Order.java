package pl.shop.order.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.order.exception.InvalidOrderStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String orderNumber;
    @Column(nullable = false, unique = true, updatable = false)
    private Long cartId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Embedded
    private Customer customer;
    @Embedded
    private Address shippingAddress;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
    @Version
    private Long version;

    public static Order place(Long cartId, Customer customer, Address shippingAddress) {
        Order order = new Order();
        order.orderNumber = UUID.randomUUID().toString();
        order.cartId = cartId;
        order.customer = customer;
        order.shippingAddress = shippingAddress;
        order.createdAt = LocalDateTime.now();
        order.status = OrderStatus.PLACED;
        return order;
    }

    public OrderItem addItem(Long productId, String productName, Price unitPrice, int quantity, List<OrderItemComponent> components) {
        validatePlaced();
        OrderItem orderItem = new OrderItem(this, productId, productName, unitPrice, quantity, components);
        items.add(orderItem);
        return orderItem;
    }

    public void cancel() {
        validatePlaced();
        status = OrderStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
    }

    public Map<Long, Integer> stockRequirements() {
        Map<Long, Integer> requirements = new LinkedHashMap<>();
        for (OrderItem item : items) {
            addRequirement(requirements, item.getStockProductIds(), item.getQuantity());
        }
        return requirements;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public int getItemCount() {
        return items.size();
    }

    public BigDecimal getTotalNet() {
        return items.stream()
                .map(item -> item.getTotalPrice().getNetPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalVat() {
        return items.stream()
                .map(item -> item.getTotalPrice().getVatAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGross() {
        return getTotalNet().add(getTotalVat());
    }

    private void addRequirement(Map<Long, Integer> requirements, List<Long> stockProductIds, int quantity) {
        for (Long productId : stockProductIds) {
            requirements.merge(productId, quantity, Integer::sum);
        }
    }

    private void validatePlaced() {
        if (isCancelled()) {
            throw new InvalidOrderStatusException(id, status);
        }
    }
}
