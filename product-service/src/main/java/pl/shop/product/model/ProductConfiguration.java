package pl.shop.product.model;

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

@Entity
@Table(name = "product_configurations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id")
    private Product component;

    private String slot;
    private boolean defaultComponent;
    private boolean multiSelect;

    ProductConfiguration(Product product, Product component, String slot, boolean defaultComponent, boolean multiSelect) {
        this.product = product;
        this.component = component;
        this.slot = slot;
        this.defaultComponent = defaultComponent;
        this.multiSelect = multiSelect;
    }
}
