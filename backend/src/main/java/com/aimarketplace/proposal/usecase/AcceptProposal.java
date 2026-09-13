package com.aimarketplace.proposal.usecase;

import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.proposal.application.ProposalApplicationService;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcceptProposal {
    private final ProposalApplicationService service;

    @Logging
    public OrderResponse execute(Long proposalId) {
        ValidationUtil.create()
                .require(proposalId != null && proposalId > 0, "proposalId", "Proposal id is required")
                .validate();
        return service.accept(proposalId);
    }
}
