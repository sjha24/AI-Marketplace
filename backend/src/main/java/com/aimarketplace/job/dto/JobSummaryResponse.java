package com.aimarketplace.job.dto;

import com.aimarketplace.job.domain.model.ExperienceLevel;
import com.aimarketplace.job.domain.model.JobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record JobSummaryResponse(
        Long id,
        Long clientId,
        Long categoryId,
        String categoryName,
        String title,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String currency,
        ExperienceLevel experienceLevel,
        JobStatus status,
        Instant createdAt,
        List<String> skillNames
) {
}
