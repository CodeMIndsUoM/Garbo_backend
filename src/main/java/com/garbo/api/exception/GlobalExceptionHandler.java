package com.garbo.api.exception;

import com.garbo.api.dto.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.EOFException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Uploaded file exceeds maximum configured size: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Uploaded file exceeds the maximum allowed size of 10MB. Please select a smaller photo."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipartException(MultipartException e) {
        Throwable rootCause = e.getRootCause();
        boolean isClientAbort = rootCause instanceof EOFException
                || (rootCause != null && rootCause.getClass().getSimpleName().contains("ClientAbortException"));

        if (isClientAbort) {
            log.warn("Client aborted upload connection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Image upload connection was interrupted. Please check your network and try again."));
        }

        log.error("Failed to parse multipart request: {}", e.getMessage(), e);
        io.sentry.Sentry.captureException(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to process uploaded file. Please ensure the file is valid and try again."));
    }

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
