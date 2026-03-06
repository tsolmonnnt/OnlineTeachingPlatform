package com.tsolmon.online_teaching_platform.auth.api.dto;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role
) {
}

