package com.aimarketplace.job.dto;

import com.aimarketplace.job.domain.model.JobStatus;

public record JobListQuery(
        String q,
        Long skillId,
        Long categoryId,
        JobStatus status,
        int page,
        int size
) {
}
