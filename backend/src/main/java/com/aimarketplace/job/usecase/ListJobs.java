package com.aimarketplace.job.usecase;

import com.aimarketplace.job.application.JobApplicationService;
import com.aimarketplace.job.domain.model.JobStatus;
import com.aimarketplace.job.dto.JobListQuery;
import com.aimarketplace.job.dto.JobSummaryResponse;
import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListJobs {
    private static final int MAX_PAGE_SIZE = 50;

    private final JobApplicationService service;

    @Logging
    public PageResponse<JobSummaryResponse> execute(String q, Long skillId, Long categoryId, JobStatus status, int page, int size) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(page >= 0, "page", "Page must be zero or greater");
        validation.require(size >= 1 && size <= MAX_PAGE_SIZE, "size", "Size must be between 1 and 50");
        validation.validate();

        JobListQuery query = new JobListQuery(q, skillId, categoryId, status, page, size);
        return service.listJobs(query);
    }
}
