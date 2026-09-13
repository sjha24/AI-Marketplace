package com.aimarketplace.user.mapper;

import com.aimarketplace.user.dto.AuthResponse;
import com.aimarketplace.user.dto.MeResponse;
import com.aimarketplace.user.dto.RegisterRequest;
import com.aimarketplace.user.entity.User;
import com.aimarketplace.user.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "displayName", source = "displayName")
    MeResponse toMeResponse(User user, String displayName);

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "displayName", source = "displayName")
    AuthResponse toAuthResponse(String accessToken, User user, String displayName);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "status", constant = "ACTIVE")
    User toUser(RegisterRequest request);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "displayName", source = "displayName", qualifiedByName = "trim")
    @Mapping(target = "headline", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "hourlyRate", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "yearsExperience", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toNewProfile(long userId, String displayName);

    @Named("normalizeEmail")
    default String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }
}
