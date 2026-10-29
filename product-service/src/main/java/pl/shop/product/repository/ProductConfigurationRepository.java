package pl.shop.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.shop.product.model.ProductConfiguration;

public interface ProductConfigurationRepository extends JpaRepository<ProductConfiguration, Long> {
}
