package com.aimarketplace.proposal.event;

public record ProposalAcceptedEvent(
        Long proposalId,
        Long jobId,
        Long clientId,
        Long freelancerId,
        Long orderId
) {
}
