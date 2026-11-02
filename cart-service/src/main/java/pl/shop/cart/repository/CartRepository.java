package pl.shop.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.shop.cart.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
