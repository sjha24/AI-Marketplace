package com.aimarketplace.user.dto;

import com.aimarketplace.user.domain.model.SkillLevel;

import java.util.List;

public record UpdateSkillsRequest(List<SkillAssignment> skills) {
    public record SkillAssignment(Long skillId, SkillLevel level) {
    }
}
