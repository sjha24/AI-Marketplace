package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import com.aimarketplace.user.application.AuthApplicationService;
import com.aimarketplace.user.dto.AuthResponse;
import com.aimarketplace.user.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUser {
    private final AuthApplicationService service;

    @Logging
    public AuthResponse execute(LoginRequest request) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(request != null, "request", "Request body is required");
        if (request != null) {
            validation.require(request.email() != null && !request.email().isBlank(), "email", "Email is required");
            validation.require(request.password() != null && !request.password().isBlank(),
                    "password", "Password is required");
        }
        validation.validate();
        return service.login(request);
    }
}
