package com.aimarketplace.user.controller;

import com.aimarketplace.user.dto.AuthResponse;
import com.aimarketplace.user.dto.LoginRequest;
import com.aimarketplace.user.dto.MeResponse;
import com.aimarketplace.user.dto.RegisterRequest;
import com.aimarketplace.user.usecase.GetCurrentUser;
import com.aimarketplace.user.usecase.LoginUser;
import com.aimarketplace.user.usecase.RegisterUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterUser registerUser;
    private final LoginUser loginUser;
    private final GetCurrentUser getCurrentUser;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return registerUser.execute(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return loginUser.execute(request);
    }

    @GetMapping("/me")
    public MeResponse me() {
        return getCurrentUser.execute();
    }
}
