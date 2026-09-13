package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import com.aimarketplace.user.application.ProfileApplicationService;
import com.aimarketplace.user.dto.ProfileResponse;
import com.aimarketplace.user.dto.UpdateSkillsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UpdateMySkills {
    private final ProfileApplicationService service;

    @Logging
    public ProfileResponse execute(UpdateSkillsRequest request) {
        ValidationUtil validation = ValidationUtil.create();
        validation.require(request != null && request.skills() != null, "skills", "Skills are required");
        if (request != null && request.skills() != null) {
            List<Long> ids = request.skills().stream()
                    .filter(assignment -> assignment != null && assignment.skillId() != null)
                    .map(UpdateSkillsRequest.SkillAssignment::skillId)
                    .toList();
            validation.require(ids.size() == request.skills().size(), "skills", "Every skill must have an id");
            Set<Long> distinct = new HashSet<>(ids);
            validation.require(distinct.size() == ids.size(), "skills", "Duplicate skills are not allowed");
            validation.require(service.allSkillsExist(ids), "skills", "One or more skills do not exist");
        }
        validation.validate();
        return service.updateMySkills(request);
    }
}
