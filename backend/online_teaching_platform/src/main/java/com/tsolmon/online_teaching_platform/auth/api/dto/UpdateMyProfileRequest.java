package com.tsolmon.online_teaching_platform.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName
) {
}
