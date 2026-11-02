package pl.shop.cart.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.cart.exception.CartAlreadyOrderedException;
import pl.shop.cart.exception.CartItemNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    public static Cart create() {
        Cart cart = new Cart();
        cart.status = CartStatus.ACTIVE;
        cart.createdAt = LocalDateTime.now();
        return cart;
    }

    public CartItem addItem(Long productId, String productName, Price unitPrice, int quantity, List<CartItemComponent> components) {
        validateActive();
        return items.stream()
                .filter(item -> item.isSameProductAs(productId, components))
                .findFirst()
                .map(existing -> increase(existing, quantity))
                .orElseGet(() -> createItem(productId, productName, unitPrice, quantity, components));
    }

    public void changeItemQuantity(Long itemId, int newQuantity) {
        validateActive();
        findItem(itemId).changeQuantity(newQuantity);
    }

    public void removeItem(Long itemId) {
        validateActive();
        boolean removed = items.removeIf(item -> Objects.equals(item.getId(), itemId));
        if (!removed) {
            throw new CartItemNotFoundException(id, itemId);
        }
    }

    public CartItem findItem(Long itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(id, itemId));
    }

    public void validateActive() {
        if (isOrdered()) {
            throw new CartAlreadyOrderedException(id);
        }
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isOrdered() {
        return status == CartStatus.ORDERED;
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

    private CartItem increase(CartItem existing, int quantity) {
        existing.increaseQuantity(quantity);
        return existing;
    }

    private CartItem createItem(Long productId, String productName, Price unitPrice, int quantity, List<CartItemComponent> components) {
        CartItem newItem = new CartItem(this, productId, productName, unitPrice, quantity, components);
        items.add(newItem);
        return newItem;
    }
}
