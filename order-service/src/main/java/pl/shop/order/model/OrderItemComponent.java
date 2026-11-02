package pl.shop.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemComponent {
    @Column(nullable = false)
    private Long configurationId;

    @Column(nullable = false)
    private Long componentProductId;

    @Column(nullable = false)
    private String slot;

    @Column(nullable = false)
    private String componentName;

    public OrderItemComponent(Long configurationId, Long componentProductId, String slot, String componentName) {
        this.configurationId = configurationId;
        this.componentProductId = componentProductId;
        this.slot = slot;
        this.componentName = componentName;
    }
}