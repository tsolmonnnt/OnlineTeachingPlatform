package com.tsolmon.online_teaching_platform.user.api.dto;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.user.entity.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}

