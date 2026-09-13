package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.user.application.ProfileApplicationService;
import com.aimarketplace.user.dto.SkillResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListSkills {
    private final ProfileApplicationService service;

    @Logging
    public List<SkillResponse> execute() {
        return service.listSkills();
    }
}
