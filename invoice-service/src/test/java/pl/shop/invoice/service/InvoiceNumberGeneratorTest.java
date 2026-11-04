package pl.shop.invoice.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import pl.shop.invoice.repository.InvoiceRepository;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InvoiceNumberGeneratorTest {
    private InvoiceNumberGenerator invoiceNumberGenerator;
    private InvoiceRepository invoiceRepository;

    @BeforeEach
    void setup() {
        this.invoiceRepository = Mockito.mock(InvoiceRepository.class);
        this.invoiceNumberGenerator = new InvoiceNumberGenerator(invoiceRepository);
    }

    @Test
    void next_firstInvoiceInMonth_numberOneReturned() {
        //given
        when(invoiceRepository.countByIssuedAtGreaterThanEqualAndIssuedAtLessThan(any(), any())).thenReturn(0L);
        //when
        String number = invoiceNumberGenerator.next(LocalDateTime.of(2026, 11, 2, 12, 0));
        //then
        Assertions.assertEquals("FV/2026/11/000001", number);
    }

    @Test
    void next_invoicesAlreadyIssuedInMonth_nextNumberReturned() {
        //given
        when(invoiceRepository.countByIssuedAtGreaterThanEqualAndIssuedAtLessThan(any(), any())).thenReturn(41L);
        //when
        String number = invoiceNumberGenerator.next(LocalDateTime.of(2026, 3, 15, 8, 30));
        //then
        Assertions.assertEquals("FV/2026/03/000042", number);
    }

    @Test
    void next_dateInMonth_countedFromFirstDayToFirstDayOfNextMonth() {
        //given
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(invoiceRepository.countByIssuedAtGreaterThanEqualAndIssuedAtLessThan(any(), any())).thenReturn(0L);
        //when
        invoiceNumberGenerator.next(LocalDateTime.of(2026, 12, 31, 23, 59));
        //then
        Mockito.verify(invoiceRepository).countByIssuedAtGreaterThanEqualAndIssuedAtLessThan(fromCaptor.capture(), toCaptor.capture());
        Assertions.assertAll(
                () -> Assertions.assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), fromCaptor.getValue()),
                () -> Assertions.assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), toCaptor.getValue())
        );
    }
}
