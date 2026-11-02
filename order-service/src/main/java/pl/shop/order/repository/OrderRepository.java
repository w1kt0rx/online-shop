package pl.shop.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.shop.order.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByCartId(Long cartId);
    Page<Order> findByCustomerEmailIgnoreCase(String email, Pageable pageable);

}
