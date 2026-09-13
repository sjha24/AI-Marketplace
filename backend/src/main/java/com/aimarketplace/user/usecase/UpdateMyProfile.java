package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import com.aimarketplace.user.application.ProfileApplicationService;
import com.aimarketplace.user.dto.ProfileResponse;
import com.aimarketplace.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class UpdateMyProfile {
    private final ProfileApplicationService service;

    @Logging
    public ProfileResponse execute(UpdateProfileRequest request) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(request != null, "request", "Request body is required");
        if (request != null) {
            validation.require(request.displayName() != null && !request.displayName().isBlank()
                            && request.displayName().trim().length() <= 120,
                    "displayName", "Display name is required and must not exceed 120 characters");
            validation.require(request.hourlyRate() == null || request.hourlyRate().compareTo(BigDecimal.ZERO) >= 0,
                    "hourlyRate", "Hourly rate cannot be negative");
            validation.require(request.yearsExperience() == null || request.yearsExperience() >= 0,
                    "yearsExperience", "Years of experience cannot be negative");
            validation.require(request.currency() == null || request.currency().isBlank()
                            || request.currency().trim().matches("[A-Za-z]{3}"),
                    "currency", "Currency must be a three-letter code");
        }
        validation.validate();
        return service.updateMyProfile(request);
    }
}
