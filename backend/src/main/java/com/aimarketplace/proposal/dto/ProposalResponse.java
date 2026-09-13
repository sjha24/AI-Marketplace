package com.aimarketplace.proposal.dto;

import com.aimarketplace.proposal.domain.model.ProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProposalResponse(
        Long id,
        Long jobId,
        String jobTitle,
        Long freelancerId,
        String freelancerDisplayName,
        Long clientId,
        String coverLetter,
        BigDecimal bidAmount,
        String currency,
        Integer deliveryDays,
        ProposalStatus status,
        boolean aiGenerated,
        Instant createdAt,
        Instant updatedAt
) {
}
