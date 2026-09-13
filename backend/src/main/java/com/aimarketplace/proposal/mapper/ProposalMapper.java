package com.aimarketplace.proposal.mapper;

import com.aimarketplace.proposal.dto.ProposalResponse;
import com.aimarketplace.proposal.dto.SubmitProposalRequest;
import com.aimarketplace.proposal.entity.Proposal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProposalMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobId", ignore = true)
    @Mapping(target = "freelancerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "aiGenerated", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "coverLetter", source = "coverLetter", qualifiedByName = "trim")
    @Mapping(target = "currency", source = "currency", qualifiedByName = "normalizeCurrency")
    Proposal toEntity(SubmitProposalRequest request);

    @Mapping(target = "jobTitle", source = "jobTitle")
    @Mapping(target = "freelancerDisplayName", source = "freelancerDisplayName")
    @Mapping(target = "clientId", source = "clientId")
    ProposalResponse toResponse(
            Proposal proposal,
            String jobTitle,
            String freelancerDisplayName,
            Long clientId
    );

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("normalizeCurrency")
    default String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }
        return currency.trim().toUpperCase();
    }
}
