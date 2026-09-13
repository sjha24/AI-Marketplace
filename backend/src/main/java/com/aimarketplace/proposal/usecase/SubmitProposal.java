package com.aimarketplace.proposal.usecase;

import com.aimarketplace.proposal.application.ProposalApplicationService;
import com.aimarketplace.proposal.dto.ProposalResponse;
import com.aimarketplace.proposal.dto.SubmitProposalRequest;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmitProposal {
    private final ProposalApplicationService service;

    @Logging
    public ProposalResponse execute(Long jobId, SubmitProposalRequest request) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(jobId != null && jobId > 0, "jobId", "Job id is required");
        ProposalRequestValidation.validateSubmit(validation, request);
        validation.validate();
        return service.submit(jobId, request);
    }
}
