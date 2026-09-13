package com.aimarketplace.job.usecase;

import com.aimarketplace.job.application.JobApplicationService;
import com.aimarketplace.job.dto.JobResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetJob {
    private final JobApplicationService service;

    @Logging
    public JobResponse execute(Long id) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(id != null, "id", "Job id is required");
        validation.validate();

        return service.getJob(id);
    }
}
