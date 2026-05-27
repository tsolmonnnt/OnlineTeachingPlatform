package com.tsolmon.online_teaching_platform.auth.api;

import com.tsolmon.online_teaching_platform.auth.api.dto.AuthResponse;
import com.tsolmon.online_teaching_platform.auth.api.dto.LoginRequest;
import com.tsolmon.online_teaching_platform.auth.api.dto.RegisterRequest;
import com.tsolmon.online_teaching_platform.auth.api.dto.UpdateMyProfileRequest;
import com.tsolmon.online_teaching_platform.auth.application.AuthService;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
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

    @PatchMapping("/me")
    public UserResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return authService.updateMe(authUser, request);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return authService.uploadAvatar(authUser, file);
    }
}
