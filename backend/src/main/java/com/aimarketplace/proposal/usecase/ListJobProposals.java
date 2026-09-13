package com.aimarketplace.proposal.usecase;

import com.aimarketplace.proposal.application.ProposalApplicationService;
import com.aimarketplace.proposal.dto.ProposalResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListJobProposals {
    private final ProposalApplicationService service;

    @Logging
    public List<ProposalResponse> execute(Long jobId) {
        ValidationUtil.create()
                .require(jobId != null && jobId > 0, "jobId", "Job id is required")
                .validate();
        return service.listForJob(jobId);
    }
}
