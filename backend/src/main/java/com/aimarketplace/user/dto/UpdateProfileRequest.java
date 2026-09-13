package com.aimarketplace.user.dto;

import java.math.BigDecimal;

public record UpdateProfileRequest(
        String displayName,
        String headline,
        String bio,
        BigDecimal hourlyRate,
        String currency,
        String location,
        String avatarUrl,
        Integer yearsExperience
) {
}
