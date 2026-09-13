package com.aimarketplace.user.application;

import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.user.dto.ProfileResponse;
import com.aimarketplace.user.dto.SkillResponse;
import com.aimarketplace.user.dto.UpdateProfileRequest;
import com.aimarketplace.user.dto.UpdateSkillsRequest;
import com.aimarketplace.user.entity.Skill;
import com.aimarketplace.user.entity.UserProfile;
import com.aimarketplace.user.entity.UserSkill;
import com.aimarketplace.user.mapper.ProfileMapper;
import com.aimarketplace.user.mapper.SkillMapper;
import com.aimarketplace.user.repository.SkillRepository;
import com.aimarketplace.user.repository.UserProfileRepository;
import com.aimarketplace.user.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;




@RequiredArgsConstructor
@Service
public class ProfileApplicationService {

    private final UserProfileRepository userProfileRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProfileMapper profileMapper;
    private final SkillMapper skillMapper;

    @Transactional(readOnly = true)
    public boolean profileExistsForCurrentUser() {
        return userProfileRepository.existsById(SecurityUtils.currentUser().id());
    }

    @Transactional(readOnly = true)
    public boolean allSkillsExist(Collection<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return true;
        }
        List<Long> distinctIds = skillIds.stream().distinct().toList();
        return skillRepository.findAllById(distinctIds).size() == distinctIds.size();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        long userId = SecurityUtils.currentUser().id();
        return toResponse(fetchProfileByUserId(userId));
    }

    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        long userId = SecurityUtils.currentUser().id();
        UserProfile profile = fetchProfileByUserId(userId);
        profileMapper.updateProfile(request, profile);
        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills() {
        return skillRepository.findAllByOrderByNameAsc().stream()
                .map(skillMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProfileResponse updateMySkills(UpdateSkillsRequest request) {
        long userId = SecurityUtils.currentUser().id();

        userSkillRepository.deleteByUserId(userId);
        userSkillRepository.flush();

        for (UpdateSkillsRequest.SkillAssignment assignment : request.skills()) {
            userSkillRepository.save(profileMapper.toUserSkill(userId, assignment));
        }

        return getMyProfile();
    }

    private UserProfile fetchProfileByUserId(long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    private ProfileResponse toResponse(UserProfile profile) {
        List<UserSkill> userSkills = userSkillRepository.findByIdUserId(profile.getUserId());
        List<Long> skillIds = userSkills.stream().map(us -> us.getId().getSkillId()).toList();
        Map<Long, Skill> skills = skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        List<ProfileResponse.ProfileSkillResponse> skillResponses = userSkills.stream()
                .map(us -> {
                    Skill skill = skills.get(us.getId().getSkillId());
                    return profileMapper.toProfileSkill(skill, us.getLevel());
                })
                .toList();

        return profileMapper.toResponse(profile, skillResponses);
    }
}
