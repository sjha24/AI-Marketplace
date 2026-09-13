package com.aimarketplace.user.dto;

import com.aimarketplace.user.domain.model.UserRole;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email,
        UserRole role,
        String displayName
) {
    public static AuthResponse bearer(String token, Long userId, String email, UserRole role, String displayName) {
        return new AuthResponse(token, "Bearer", userId, email, role, displayName);
    }
}
