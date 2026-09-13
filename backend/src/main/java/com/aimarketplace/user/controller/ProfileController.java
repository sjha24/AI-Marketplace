package com.aimarketplace.user.controller;

import com.aimarketplace.user.dto.ProfileResponse;
import com.aimarketplace.user.dto.SkillResponse;
import com.aimarketplace.user.dto.UpdateProfileRequest;
import com.aimarketplace.user.dto.UpdateSkillsRequest;
import com.aimarketplace.user.usecase.GetMyProfile;
import com.aimarketplace.user.usecase.ListSkills;
import com.aimarketplace.user.usecase.UpdateMyProfile;
import com.aimarketplace.user.usecase.UpdateMySkills;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final GetMyProfile getMyProfile;
    private final UpdateMyProfile updateMyProfile;
    private final ListSkills listSkills;
    private final UpdateMySkills updateMySkills;

    @GetMapping("/api/profiles/me")
    public ProfileResponse getProfile() {
        return getMyProfile.execute();
    }

    @PutMapping("/api/profiles/me")
    public ProfileResponse updateProfile(@RequestBody UpdateProfileRequest request) {
        return updateMyProfile.execute(request);
    }

    @GetMapping("/api/skills")
    public List<SkillResponse> skills() {
        return listSkills.execute();
    }

    @PutMapping("/api/profiles/me/skills")
    public ProfileResponse updateSkills(@RequestBody UpdateSkillsRequest request) {
        return updateMySkills.execute(request);
    }
}
