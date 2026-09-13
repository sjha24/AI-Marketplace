package com.aimarketplace.user.mapper;

import com.aimarketplace.user.domain.model.SkillLevel;
import com.aimarketplace.user.dto.ProfileResponse;
import com.aimarketplace.user.dto.UpdateProfileRequest;
import com.aimarketplace.user.dto.UpdateSkillsRequest;
import com.aimarketplace.user.entity.Skill;
import com.aimarketplace.user.entity.UserProfile;
import com.aimarketplace.user.entity.UserSkill;
import com.aimarketplace.user.entity.UserSkillId;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(target = "skills", source = "skills")
    ProfileResponse toResponse(UserProfile profile, List<ProfileResponse.ProfileSkillResponse> skills);

    @Mapping(target = "skillId", source = "skill.id")
    @Mapping(target = "name", source = "skill.name")
    @Mapping(target = "slug", source = "skill.slug")
    @Mapping(target = "level", source = "level")
    ProfileResponse.ProfileSkillResponse toProfileSkill(Skill skill, SkillLevel level);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "displayName", source = "displayName", qualifiedByName = "trim")
    @Mapping(target = "headline", source = "headline", qualifiedByName = "blankToNull")
    @Mapping(target = "bio", source = "bio", qualifiedByName = "blankToNull")
    @Mapping(target = "location", source = "location", qualifiedByName = "blankToNull")
    @Mapping(target = "avatarUrl", source = "avatarUrl", qualifiedByName = "blankToNull")
    void updateProfile(UpdateProfileRequest request, @MappingTarget UserProfile profile);

    @AfterMapping
    default void applyCurrency(UpdateProfileRequest request, @MappingTarget UserProfile profile) {
        if (request.currency() != null && !request.currency().isBlank()) {
            profile.setCurrency(request.currency().toUpperCase());
        }
    }

    default UserSkill toUserSkill(long userId, UpdateSkillsRequest.SkillAssignment assignment) {
        UserSkill entity = new UserSkill();
        entity.setId(new UserSkillId(userId, assignment.skillId()));
        entity.setLevel(assignment.level() == null ? SkillLevel.INTERMEDIATE : assignment.level());
        return entity;
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("blankToNull")
    default String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
