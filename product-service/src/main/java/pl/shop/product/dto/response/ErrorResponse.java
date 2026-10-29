package pl.shop.product.dto.response;

import java.time.LocalDateTime;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String errorCode,
        String message
) {
    public ErrorResponse(int status, String errorCode, String message) {
        this(LocalDateTime.now(), status, errorCode, message);
    }
}