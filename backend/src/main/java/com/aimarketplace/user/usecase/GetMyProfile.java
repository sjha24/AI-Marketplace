package com.aimarketplace.user.usecase;

import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.user.application.ProfileApplicationService;
import com.aimarketplace.user.dto.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetMyProfile {
    private final ProfileApplicationService service;

    @Logging
    public ProfileResponse execute() {
        if (!service.profileExistsForCurrentUser()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        return service.getMyProfile();
    }
}
