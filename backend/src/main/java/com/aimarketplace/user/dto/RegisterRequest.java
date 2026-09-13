package com.aimarketplace.user.dto;

import com.aimarketplace.user.domain.model.UserRole;

public record RegisterRequest(String email, String password, String displayName, UserRole role) {
}
