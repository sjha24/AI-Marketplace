package com.aimarketplace.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, String> details;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(HttpStatus status, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static ApiException ofField(HttpStatus status, String message, String field, String detail) {
        return new ApiException(status, message, Map.of(field, detail));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
