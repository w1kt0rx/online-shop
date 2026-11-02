package pl.shop.order.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
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
import pl.shop.order.dto.response.ErrorResponse;
import pl.shop.order.exception.OrderServiceException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private static final String REMOTE_ERROR_CODE = "REMOTE_SERVICE_ERROR";
    private final ObjectMapper objectMapper;

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(OrderServiceException ex) {
        log.warn("Order service exception occurred: code = {}, message = {}, status = {}",
                ex.getErrorCode(), ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getStatus().value(), ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(RetryableException ex) {
        log.error("Remote service unavailable after retries", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, "SERVICE_UNAVAILABLE", "Remote service is unavailable, try again later"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "ARGUMENT_NOT_VALID", "Validation failed: " + errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "MALFORMED_REQUEST", "Request body is invalid or malformed"));
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "DATA_CONFLICT", "Operation violates data constraints"));
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

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        log.warn("Remote call failed: status = {}, message = {}", ex.status(), ex.getMessage());
        if (isServerError(ex.status())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse(503, REMOTE_ERROR_CODE, "Remote service failed, try again later"));
        }
        HttpStatus status = resolveStatus(ex.status());
        JsonNode body = readRemoteBody(ex);
        String errorCode = textOrDefault(body, "errorCode", REMOTE_ERROR_CODE);
        String message = textOrDefault(body, "message", "Remote service returned status " + ex.status());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), errorCode, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "Unexpected error occurred"));
    }

    private boolean isServerError(int status) {
        return status <= 0 || status >= 500;
    }

    private HttpStatus resolveStatus(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved == null ? HttpStatus.BAD_GATEWAY : resolved;
    }

    private JsonNode readRemoteBody(FeignException ex) {
        String content = ex.contentUTF8();
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException parseException) {
            log.warn("Couldn't parse remote error body: {}", content);
            return objectMapper.createObjectNode();
        }
    }

    private static String textOrDefault(JsonNode body, String field, String defaultValue) {
        JsonNode node = body.get(field);
        return node == null || node.isNull() ? defaultValue : node.asText();
    }
}
