package com.aimarketplace.proposal.dto;

import java.math.BigDecimal;

public record SubmitProposalRequest(
        String coverLetter,
        BigDecimal bidAmount,
        String currency,
        Integer deliveryDays
) {
}
