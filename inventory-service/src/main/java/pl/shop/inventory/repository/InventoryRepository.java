package pl.shop.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.shop.inventory.model.StockItem;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<StockItem, Long> {
    boolean existsByProductId(Long productId);
    Optional<StockItem> findByProductId(Long productId);
    List<StockItem> findAllByProductIdIn(List<Long> productIds);
}
