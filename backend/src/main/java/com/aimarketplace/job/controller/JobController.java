package com.aimarketplace.job.controller;

import com.aimarketplace.job.domain.model.JobStatus;
import com.aimarketplace.job.dto.CreateJobRequest;
import com.aimarketplace.job.dto.JobResponse;
import com.aimarketplace.job.dto.JobSummaryResponse;
import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.job.dto.UpdateJobRequest;
import com.aimarketplace.job.usecase.CancelJob;
import com.aimarketplace.job.usecase.CreateJob;
import com.aimarketplace.job.usecase.GetJob;
import com.aimarketplace.job.usecase.GetMyJobs;
import com.aimarketplace.job.usecase.ListJobs;
import com.aimarketplace.job.usecase.UpdateJob;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class JobController {
    private final CreateJob createJob;
    private final ListJobs listJobs;
    private final GetMyJobs getMyJobs;
    private final GetJob getJob;
    private final UpdateJob updateJob;
    private final CancelJob cancelJob;

    @PostMapping("/api/jobs")
    @PreAuthorize("hasRole('CLIENT')")
    public JobResponse create(@RequestBody CreateJobRequest request) {
        return createJob.execute(request);
    }

    @GetMapping("/api/jobs")
    public PageResponse<JobSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "OPEN") JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return listJobs.execute(q, skillId, categoryId, status, page, size);
    }

    @GetMapping("/api/jobs/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public PageResponse<JobSummaryResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getMyJobs.execute(page, size);
    }

    @GetMapping("/api/jobs/{id}")
    public JobResponse get(@PathVariable Long id) {
        return getJob.execute(id);
    }

    @PutMapping("/api/jobs/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    public JobResponse update(@PathVariable Long id, @RequestBody UpdateJobRequest request) {
        return updateJob.execute(id, request);
    }

    @PostMapping("/api/jobs/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public JobResponse cancel(@PathVariable Long id) {
        return cancelJob.execute(id);
    }
}
