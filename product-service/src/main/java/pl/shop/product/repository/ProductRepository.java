package pl.shop.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.shop.product.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);
}
