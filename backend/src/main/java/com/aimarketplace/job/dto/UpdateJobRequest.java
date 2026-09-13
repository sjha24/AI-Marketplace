package com.aimarketplace.job.dto;

import com.aimarketplace.job.domain.model.ExperienceLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UpdateJobRequest(
        Long categoryId,
        String title,
        String description,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String currency,
        ExperienceLevel experienceLevel,
        Instant closesAt,
        List<Long> skillIds
) {
}
