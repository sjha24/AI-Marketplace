package com.aimarketplace.shared.validation;

import com.aimarketplace.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationUtil {
    private final Map<String, String> errors = new LinkedHashMap<>();

    private ValidationUtil() {
    }

    public static ValidationUtil create() {
        return new ValidationUtil();
    }

    public ValidationUtil require(boolean condition, String field, String message) {
        if (!condition) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public ValidationUtil reject(String field, String message) {
        errors.putIfAbsent(field, message);
        return this;
    }

    public void validate() {
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please fix the highlighted fields", errors);
        }
    }
}
