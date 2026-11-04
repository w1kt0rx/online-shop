package pl.shop.invoice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.shop.invoice.model.Invoice;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByOrderId(Long orderId);

    Optional<Invoice> findByOrderId(Long orderId);

    Page<Invoice> findByBuyerEmailIgnoreCase(String email, Pageable pageable);

    long countByIssuedAtGreaterThanEqualAndIssuedAtLessThan(LocalDateTime from, LocalDateTime to);
}
