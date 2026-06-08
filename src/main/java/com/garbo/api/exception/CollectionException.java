package com.garbo.api.exception;

import org.springframework.http.HttpStatus;

public class CollectionException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public CollectionException(HttpStatus status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
