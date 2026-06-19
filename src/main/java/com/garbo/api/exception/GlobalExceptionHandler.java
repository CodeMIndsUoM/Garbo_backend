package com.garbo.api.exception;

import com.garbo.api.dto.common.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        io.sentry.Sentry.captureException(e);
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage() == null ? "Request failed" : e.getMessage()));
    }

    @ExceptionHandler(CollectionException.class)
    public ResponseEntity<ApiResponse<Object>> handleCollectionException(CollectionException e) {
        io.sentry.Sentry.captureException(e);
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        io.sentry.Sentry.captureException(e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "VALIDATION_ERROR"));
    }
}
