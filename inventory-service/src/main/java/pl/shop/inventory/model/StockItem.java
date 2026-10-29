package pl.shop.inventory.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.inventory.exception.InsufficientStockException;
import pl.shop.inventory.exception.InvalidQuantityException;

@Entity
@Table(name = "stock_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long productId;
    @Column(nullable = false)
    private int quantity;
    @Version
    private Long version;

    public StockItem(Long productId) {
        validateProductId(productId);
        this.productId = productId;
        this.quantity = 0;
    }

    public void changeQuantity(int newQuantity) {
        validateNotNegative(newQuantity);
        this.quantity = newQuantity;
    }

    public void increase(int amount) {
        validatePositive(amount);
        this.quantity += amount;
    }

    public void decrease(int amount) {
        validatePositive(amount);
        if (!isAvailable(amount)) {
            throw new InsufficientStockException(productId, amount, quantity);
        }
        this.quantity -= amount;
    }

    public boolean isAvailable(int amount) {
        return quantity >= amount;
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new InvalidQuantityException("Product id must be positive: " + productId);
        }
    }

    private void validateNotNegative(int value) {
        if (value < 0) {
            throw new InvalidQuantityException("Quantity cannot be negative: " + value);
        }
    }

    private void validatePositive(int value) {
        if (value <= 0) {
            throw new InvalidQuantityException("Amount must be greater than 0: " + value);
        }
    }
}