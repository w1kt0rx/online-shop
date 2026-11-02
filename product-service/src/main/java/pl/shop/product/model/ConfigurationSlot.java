package pl.shop.product.model;

import pl.shop.product.exception.InvalidConfigurationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

class ConfigurationSlot {
    private final String name;
    private final List<ProductConfiguration> options;

    ConfigurationSlot(String name, List<ProductConfiguration> options) {
        this.name = name;
        this.options = List.copyOf(options);
    }

    Selection select(Set<Long> requestedConfigurationIds) {
        List<ProductConfiguration> chosen = chosenFrom(requestedConfigurationIds);
        validateSelectionSize(chosen);
        List<ProductConfiguration> selection = chosen.isEmpty() ? defaults() : chosen;
        return new Selection(selection, priceDifference(selection));
    }

    private List<ProductConfiguration> chosenFrom(Set<Long> requestedConfigurationIds) {
        return options.stream()
                .filter(option -> requestedConfigurationIds.contains(option.getId()))
                .toList();
    }

    private List<ProductConfiguration> defaults() {
        return options.stream()
                .filter(ProductConfiguration::isDefaultComponent)
                .toList();
    }

    private void validateSelectionSize(List<ProductConfiguration> chosen) {
        if (chosen.size() > 1 && !isMultiSelect()) {
            throw new InvalidConfigurationException("Only one component can be selected in slot: " + name);
        }
    }

    private boolean isMultiSelect() {
        return options.getFirst().isMultiSelect();
    }

    private BigDecimal priceDifference(List<ProductConfiguration> selection) {
        return sumOfNetPrices(selection).subtract(sumOfNetPrices(defaults()));
    }

    private static BigDecimal sumOfNetPrices(List<ProductConfiguration> configurations) {
        return configurations.stream()
                .map(configuration -> configuration.getComponent().getPrice().getNetPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    record Selection(List<ProductConfiguration> configurations, BigDecimal priceDifference) {
    }
}
