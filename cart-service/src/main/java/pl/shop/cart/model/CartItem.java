package pl.shop.cart.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.cart.exception.InvalidQuantityException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {
    private static final int MAX_QUANTITY = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Embedded
    private Price unitPrice;

    @Column(nullable = false)
    private int quantity;

    @ElementCollection
    @CollectionTable(name = "cart_item_components", joinColumns = @JoinColumn(name = "cart_item_id"))
    private List<CartItemComponent> components = new ArrayList<>();

    CartItem(Cart cart, Long productId, String productName, Price unitPrice, int quantity, List<CartItemComponent> components) {
        validateQuantity(quantity);
        this.cart = cart;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.components = new ArrayList<>(components);
    }

    void increaseQuantity(int amount) {
        validateQuantity(quantity + amount);
        this.quantity += amount;
    }

    void changeQuantity(int newQuantity) {
        validateQuantity(newQuantity);
        this.quantity = newQuantity;
    }

    boolean isSameProductAs(Long otherProductId, List<CartItemComponent> otherComponents) {
        return Objects.equals(productId, otherProductId)
                && getConfigurationIds().equals(configurationIdsOf(otherComponents));
    }

    public Price getTotalPrice() {
        return unitPrice.multiply(quantity);
    }

    public List<Long> getStockProductIds() {
        return Stream.concat(
                Stream.of(productId),
                components.stream().map(CartItemComponent::getComponentProductId)
        ).toList();
    }

    public Set<Long> getConfigurationIds() {
        return configurationIdsOf(components);
    }

    public List<CartItemComponent> getComponents() {
        return Collections.unmodifiableList(components);
    }

    private static Set<Long> configurationIdsOf(List<CartItemComponent> components) {
        return components.stream()
                .map(CartItemComponent::getConfigurationId)
                .collect(Collectors.toSet());
    }

    private void validateQuantity(int value) {
        if (value < 1 || value > MAX_QUANTITY) {
            throw new InvalidQuantityException(value, MAX_QUANTITY);
        }
    }
}
