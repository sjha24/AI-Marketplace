package com.aimarketplace.job.usecase;

import com.aimarketplace.job.application.JobApplicationService;
import com.aimarketplace.job.dto.CreateJobRequest;
import com.aimarketplace.job.dto.JobResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateJob {
    private final JobApplicationService service;

    @Logging
    public JobResponse execute(CreateJobRequest request) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(request != null, "request", "Request body is required");
        if (request != null) {
            JobRequestValidation.validateCommon(
                    validation,
                    request.categoryId(),
                    request.title(),
                    request.description(),
                    request.budgetMin(),
                    request.budgetMax(),
                    request.currency(),
                    request.experienceLevel(),
                    request.skillIds()
            );
        }
        validation.validate();

        if (!service.categoryExists(request.categoryId())) {
            throw ApiException.ofField(HttpStatus.BAD_REQUEST, "Please fix the highlighted fields",
                    "categoryId", "Selected category does not exist");
        }
        if (!service.allSkillsExist(request.skillIds())) {
            throw ApiException.ofField(HttpStatus.BAD_REQUEST, "Please fix the highlighted fields",
                    "skillIds", "One or more selected skills do not exist");
        }

        return service.createJob(request);
    }
}
