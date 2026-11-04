package pl.shop.invoice.exception;

import org.springframework.http.HttpStatus;

public class InvalidInvoiceLineException extends InvoiceServiceException {
    public InvalidInvoiceLineException(String message) {
        super("INVALID_INVOICE_LINE", message, HttpStatus.BAD_REQUEST);
    }
}
