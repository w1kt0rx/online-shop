package pl.shop.invoice.exception;

import org.springframework.http.HttpStatus;
import pl.shop.invoice.model.InvoiceStatus;

public class InvalidInvoiceStatusException extends InvoiceServiceException {
    public InvalidInvoiceStatusException(String invoiceNumber, InvoiceStatus status) {
        super("INVALID_INVOICE_STATUS", "Invoice " + invoiceNumber + " cannot be modified in status " + status, HttpStatus.CONFLICT);
    }
}
