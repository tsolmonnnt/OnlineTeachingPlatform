package com.tsolmon.online_teaching_platform.auth.api.dto;

import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        UserResponse user
) {
}

