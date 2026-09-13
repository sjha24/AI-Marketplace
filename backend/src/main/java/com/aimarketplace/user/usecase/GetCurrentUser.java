package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.user.application.AuthApplicationService;
import com.aimarketplace.user.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCurrentUser {
    private final AuthApplicationService service;

    @Logging
    public MeResponse execute() {
        return service.getCurrentUser();
    }
}
