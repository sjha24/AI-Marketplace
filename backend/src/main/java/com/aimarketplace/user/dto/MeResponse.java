package com.aimarketplace.user.dto;

import com.aimarketplace.user.domain.model.UserRole;
import com.aimarketplace.user.domain.model.UserStatus;

public record MeResponse(
        Long id,
        String email,
        UserRole role,
        UserStatus status,
        String displayName
) {
}
