package pl.shop.invoice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.shop.invoice.repository.InvoiceRepository;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {
    private static final String FORMAT = "FV/%d/%02d/%06d";

    private final InvoiceRepository invoiceRepository;

    public String next(LocalDateTime issuedAt) {
        YearMonth month = YearMonth.from(issuedAt);
        long issuedInMonth = invoiceRepository.countByIssuedAtGreaterThanEqualAndIssuedAtLessThan(
                month.atDay(1).atStartOfDay(),
                month.plusMonths(1).atDay(1).atStartOfDay()
        );
        return String.format(FORMAT, month.getYear(), month.getMonthValue(), issuedInMonth + 1);
    }
}
