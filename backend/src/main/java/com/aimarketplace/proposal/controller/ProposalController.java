package com.aimarketplace.proposal.controller;

import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.proposal.dto.ProposalResponse;
import com.aimarketplace.proposal.dto.SubmitProposalRequest;
import com.aimarketplace.proposal.usecase.AcceptProposal;
import com.aimarketplace.proposal.usecase.GetMyProposals;
import com.aimarketplace.proposal.usecase.ListJobProposals;
import com.aimarketplace.proposal.usecase.SubmitProposal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProposalController {
    private final SubmitProposal submitProposal;
    private final ListJobProposals listJobProposals;
    private final GetMyProposals getMyProposals;
    private final AcceptProposal acceptProposal;

    @PostMapping("/api/jobs/{jobId}/proposals")
    @PreAuthorize("hasRole('FREELANCER')")
    public ProposalResponse submit(@PathVariable Long jobId, @RequestBody SubmitProposalRequest request) {
        return submitProposal.execute(jobId, request);
    }

    @GetMapping("/api/jobs/{jobId}/proposals")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ProposalResponse> listForJob(@PathVariable Long jobId) {
        return listJobProposals.execute(jobId);
    }

    @GetMapping("/api/proposals/mine")
    @PreAuthorize("hasRole('FREELANCER')")
    public PageResponse<ProposalResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getMyProposals.execute(page, size);
    }

    @PostMapping("/api/proposals/{id}/accept")
    @PreAuthorize("hasRole('CLIENT')")
    public OrderResponse accept(@PathVariable Long id) {
        return acceptProposal.execute(id);
    }
}
