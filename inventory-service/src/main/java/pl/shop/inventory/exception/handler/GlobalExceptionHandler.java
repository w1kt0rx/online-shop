package pl.shop.inventory.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.shop.inventory.dto.response.ErrorResponse;
import pl.shop.inventory.exception.InventoryServiceException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryServiceException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(InventoryServiceException ex) {
        log.warn("Product service exception occurred: code='{}', message='{}', status={}",
                ex.getErrorCode(), ex.getMessage(), ex.getStatus());
        return status(ex);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getStatusCode().value(), "ARGUMENT_NOT_VALID", "Validation failed: " + errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "MALFORMED_REQUEST", "Request body is invalid or malformed"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "DATA_CONFLICT", "Operation violates data constraints"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "Unexpected error occurred"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "TYPE_MISMATCH", "Invalid value for parameter: " + ex.getName()));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(PropertyReferenceException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "INVALID_SORT_PROPERTY", "Cannot sort by: " + ex.getPropertyName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "NOT_FOUND", "Resource not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(405, "METHOD_NOT_ALLOWED", "Method not allowed: " + ex.getMethod()));
    }


    private ResponseEntity<ErrorResponse> status(InventoryServiceException ex) {
        ErrorResponse body = new ErrorResponse(ex.getStatus().value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
