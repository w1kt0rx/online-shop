package pl.shop.product.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.shop.product.exception.ConfigurationNotFoundException;
import pl.shop.product.exception.InvalidConfigurationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private String vendor;
    @Embedded
    private Price price;
    private boolean sellableSeparately;
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductConfiguration> configurations = new ArrayList<>();

    public Product(String name, String category, String vendor, Price price, boolean sellableSeparately) {
        this.name = name;
        this.category = category;
        this.vendor = vendor;
        this.price = price;
        this.sellableSeparately = sellableSeparately;
    }

    public void update(String name, String category, String vendor, Price price, boolean sellableSeparately) {
        this.name = name;
        this.category = category;
        this.vendor = vendor;
        this.price = price;
        this.sellableSeparately = sellableSeparately;
    }

    public ProductConfiguration addConfiguration(Product component, String slot, boolean defaultComponent, boolean multiSelect) {
        validateNotSelf(component);
        validateNotAlreadyInSlot(component, slot);
        validateSingleDefault(slot, defaultComponent);
        validateMultiSelectConsistent(slot, multiSelect);

        ProductConfiguration configuration = new ProductConfiguration(this, component, slot, defaultComponent, multiSelect);
        configurations.add(configuration);
        return configuration;
    }

    public void removeConfiguration(Long configurationId) {
        boolean removed = configurations.removeIf(c -> Objects.equals(c.getId(), configurationId));
        if (!removed) {
            throw new ConfigurationNotFoundException(configurationId);
        }
    }

    public List<ProductConfiguration> getConfigurations() {
        return Collections.unmodifiableList(configurations);
    }

    public boolean isConfigurable() {
        return !configurations.isEmpty();
    }

    private void validateNotAlreadyInSlot(Product component, String slot) {
        boolean exists = configurations.stream()
                .anyMatch(c -> c.getSlot().equals(slot)
                        && Objects.equals(c.getComponent().getId(), component.getId()));
        if (exists) {
            throw new InvalidConfigurationException("Component already added to slot: " + slot);
        }
    }

    private void validateNotSelf(Product component) {
        if (component == this || (component.id != null && component.id.equals(this.id))) {
            throw new InvalidConfigurationException("Product cannot be its own component");
        }
    }

    private void validateSingleDefault(String slot, boolean defaultComponent) {
        boolean exists = configurations.stream()
                .anyMatch(c -> c.getSlot().equals(slot) && c.isDefaultComponent());
        if (exists && defaultComponent) {
            throw new InvalidConfigurationException("Slot already has a default component: " + slot);
        }
    }

    private void validateMultiSelectConsistent(String slot, boolean multiSelect) {
        boolean inconsistent = configurations.stream()
                .anyMatch(c -> c.getSlot().equals(slot) && c.isMultiSelect() != multiSelect);
        if (inconsistent) {
            throw new InvalidConfigurationException("multiSelect must be the same for whole: " + slot);
        }
    }
}
