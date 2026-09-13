package com.aimarketplace.proposal.usecase;

import com.aimarketplace.proposal.dto.SubmitProposalRequest;
import com.aimarketplace.shared.validation.ValidationUtil;

import java.math.BigDecimal;

final class ProposalRequestValidation {
    private ProposalRequestValidation() {
    }

    static void validateSubmit(ValidationUtil validation, SubmitProposalRequest request) {
        validation.require(request != null, "request", "Request body is required");
        if (request == null) {
            return;
        }

        String cover = request.coverLetter() == null ? "" : request.coverLetter().trim();
        validation.require(!cover.isEmpty(), "coverLetter", "Cover letter is required");
        validation.require(cover.length() <= 5000, "coverLetter", "Cover letter must be at most 5000 characters");

        validation.require(request.bidAmount() != null, "bidAmount", "Bid amount is required");
        if (request.bidAmount() != null) {
            validation.require(request.bidAmount().compareTo(BigDecimal.ZERO) > 0, "bidAmount", "Bid amount must be greater than 0");
            validation.require(request.bidAmount().scale() <= 2, "bidAmount", "Bid amount can have at most 2 decimal places");
        }

        String currency = request.currency() == null ? "" : request.currency().trim();
        validation.require(!currency.isEmpty(), "currency", "Currency is required");
        validation.require(currency.matches("(?i)[A-Z]{3}"), "currency", "Currency must be a 3-letter code");

        validation.require(request.deliveryDays() != null, "deliveryDays", "Delivery days is required");
        if (request.deliveryDays() != null) {
            validation.require(request.deliveryDays() >= 1, "deliveryDays", "Delivery days must be at least 1");
            validation.require(request.deliveryDays() <= 365, "deliveryDays", "Delivery days must be at most 365");
        }
    }
}
