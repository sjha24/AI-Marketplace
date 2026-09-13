package com.aimarketplace.job.usecase;

import com.aimarketplace.job.application.JobApplicationService;
import com.aimarketplace.job.dto.CategoryResponse;
import com.aimarketplace.shared.aop.Logging;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListCategories {
    private final JobApplicationService service;

    @Logging
    public List<CategoryResponse> execute() {
        return service.listCategories();
    }
}
