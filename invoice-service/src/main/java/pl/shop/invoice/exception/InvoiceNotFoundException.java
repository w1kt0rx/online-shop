package pl.shop.invoice.exception;

import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends InvoiceServiceException {

    private InvoiceNotFoundException(String message) {
        super("INVOICE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public static InvoiceNotFoundException byId(Long invoiceId) {
        return new InvoiceNotFoundException("Invoice with id = " + invoiceId + " doesn't exist");
    }

    public static InvoiceNotFoundException forOrder(Long orderId) {
        return new InvoiceNotFoundException("Invoice for order with id = " + orderId + " doesn't exist");
    }
}
