package com.aimarketplace.user.application;

import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.JwtService;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.shared.security.UserPrincipal;
import com.aimarketplace.user.domain.model.UserStatus;
import com.aimarketplace.user.dto.AuthResponse;
import com.aimarketplace.user.dto.LoginRequest;
import com.aimarketplace.user.dto.MeResponse;
import com.aimarketplace.user.dto.RegisterRequest;
import com.aimarketplace.user.entity.User;
import com.aimarketplace.user.entity.UserProfile;
import com.aimarketplace.user.mapper.AuthMapper;
import com.aimarketplace.user.repository.UserProfileRepository;
import com.aimarketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;

    @Transactional(readOnly = true)
    public boolean existsByEmailIgnoreCase(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            User user = authMapper.toUser(request);
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            userRepository.save(user);

            UserProfile profile = authMapper.toNewProfile(user.getId(), request.displayName());
            userProfileRepository.save(profile);

            String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
            return authMapper.toAuthResponse(token, user, profile.getDisplayName());
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.ofField(
                    HttpStatus.CONFLICT,
                    "Please fix the highlighted fields",
                    "email",
                    "This email is already registered"
            );
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        UserProfile profile = userProfileRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Profile missing"));

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return authMapper.toAuthResponse(token, user, profile.getDisplayName());
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
        return authMapper.toMeResponse(user, profile.getDisplayName());
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return me(principal.id());
    }
}
