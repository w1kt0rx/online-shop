package pl.shop.product.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.shop.product.exception.ConfigurationNotFoundException;
import pl.shop.product.exception.InvalidConfigurationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
        boolean removed = configurations.removeIf(configuration -> Objects.equals(configuration.getId(), configurationId));
        if (!removed) {
            throw new ConfigurationNotFoundException(configurationId);
        }
    }

    public Quote quote(Collection<Long> requestedConfigurationIds) {
        Set<Long> requested = Set.copyOf(requestedConfigurationIds);
        validateConfigurationsBelongToProduct(requested);
        List<ConfigurationSlot.Selection> selections = slots().stream()
                .map(slot -> slot.select(requested))
                .toList();
        List<ProductConfiguration> selected = selectedConfigurations(selections);
        Price unitPrice = price.withNetPrice(price.getNetPrice().add(sumOfPriceDifferences(selections)));
        return new Quote(selected, unitPrice, stockProductIdsFor(selected));
    }

    public List<ProductConfiguration> getConfigurations() {
        return Collections.unmodifiableList(configurations);
    }

    public boolean isConfigurable() {
        return !configurations.isEmpty();
    }

    private List<ConfigurationSlot> slots() {
        return configurations.stream()
                .collect(Collectors.groupingBy(ProductConfiguration::getSlot, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new ConfigurationSlot(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void validateConfigurationsBelongToProduct(Set<Long> requested) {
        Set<Long> ownIds = configurations.stream()
                .map(ProductConfiguration::getId)
                .collect(Collectors.toSet());
        List<Long> foreign = requested.stream()
                .filter(requestedId -> !ownIds.contains(requestedId))
                .sorted()
                .toList();
        if (!foreign.isEmpty()) {
            throw new InvalidConfigurationException("Configurations " + foreign + " do not belong to product id = " + id);
        }
    }

    private static List<ProductConfiguration> selectedConfigurations(List<ConfigurationSlot.Selection> selections) {
        return selections.stream()
                .flatMap(selection -> selection.configurations().stream())
                .toList();
    }

    private static BigDecimal sumOfPriceDifferences(List<ConfigurationSlot.Selection> selections) {
        return selections.stream()
                .map(ConfigurationSlot.Selection::priceDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Long> stockProductIdsFor(List<ProductConfiguration> selected) {
        return Stream.concat(
                Stream.of(id),
                selected.stream().map(configuration -> configuration.getComponent().getId())
        ).toList();
    }

    private void validateNotSelf(Product component) {
        if (component == this || (component.id != null && component.id.equals(id))) {
            throw new InvalidConfigurationException("Product cannot be its own component");
        }
    }

    private void validateNotAlreadyInSlot(Product component, String slot) {
        boolean exists = configurations.stream()
                .anyMatch(configuration -> configuration.getSlot().equals(slot)
                        && Objects.equals(configuration.getComponent().getId(), component.getId()));
        if (exists) {
            throw new InvalidConfigurationException("Component already added to slot: " + slot);
        }
    }

    private void validateSingleDefault(String slot, boolean defaultComponent) {
        boolean defaultExists = configurations.stream()
                .anyMatch(configuration -> configuration.getSlot().equals(slot) && configuration.isDefaultComponent());
        if (defaultComponent && defaultExists) {
            throw new InvalidConfigurationException("Slot already has a default component: " + slot);
        }
    }

    private void validateMultiSelectConsistent(String slot, boolean multiSelect) {
        boolean inconsistent = configurations.stream()
                .anyMatch(configuration -> configuration.getSlot().equals(slot) && configuration.isMultiSelect() != multiSelect);
        if (inconsistent) {
            throw new InvalidConfigurationException("multiSelect must be the same for whole slot: " + slot);
        }
    }
}
