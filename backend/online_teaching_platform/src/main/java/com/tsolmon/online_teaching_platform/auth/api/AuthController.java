package com.tsolmon.online_teaching_platform.auth.api;

import com.tsolmon.online_teaching_platform.auth.api.dto.AuthResponse;
import com.tsolmon.online_teaching_platform.auth.api.dto.LoginRequest;
import com.tsolmon.online_teaching_platform.auth.api.dto.RegisterRequest;
import com.tsolmon.online_teaching_platform.auth.application.AuthService;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return authService.me(authUser);
    }
}
