package com.aimarketplace.user.dto;

import com.aimarketplace.user.domain.model.SkillLevel;

import java.math.BigDecimal;
import java.util.List;

public record ProfileResponse(
        Long userId,
        String displayName,
        String headline,
        String bio,
        BigDecimal hourlyRate,
        String currency,
        String location,
        String avatarUrl,
        Integer yearsExperience,
        List<ProfileSkillResponse> skills
) {
    public record ProfileSkillResponse(Long skillId, String name, String slug, SkillLevel level) {
    }
}
