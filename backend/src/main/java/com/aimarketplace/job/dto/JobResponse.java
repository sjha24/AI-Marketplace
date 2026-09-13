package com.aimarketplace.job.dto;

import com.aimarketplace.job.domain.model.ExperienceLevel;
import com.aimarketplace.job.domain.model.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobResponse(
        Long id,
        Long clientId,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String currency,
        ExperienceLevel experienceLevel,
        JobStatus status,
        Instant closesAt,
        Instant createdAt,
        Instant updatedAt,
        List<JobSkillResponse> skills
) {
    public record JobSkillResponse(Long skillId, String name, String slug) {
    }
}
