package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import com.aimarketplace.user.application.AuthApplicationService;
import com.aimarketplace.user.domain.model.UserRole;
import com.aimarketplace.user.dto.AuthResponse;
import com.aimarketplace.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class RegisterUser {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AuthApplicationService service;

    @Logging
    public AuthResponse execute(RegisterRequest request) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(request != null, "request", "Request body is required");
        if (request != null) {
            validation.require(request.email() != null && EMAIL.matcher(request.email().trim()).matches(),
                    "email", "Enter a valid email");
            validation.require(request.password() != null && request.password().length() >= 8,
                    "password", "Password must contain at least 8 characters");
            validation.require(request.displayName() != null && !request.displayName().isBlank()
                            && request.displayName().trim().length() <= 120,
                    "displayName", "Display name is required and must not exceed 120 characters");
            validation.require(request.role() == UserRole.CLIENT || request.role() == UserRole.FREELANCER,
                    "role", "Role must be CLIENT or FREELANCER");
            if (request.email() != null && service.existsByEmailIgnoreCase(request.email().trim())) {
                validation.reject("email", "This email is already registered");
            }
        }
        validation.validate();
        return service.register(request);
    }
}
