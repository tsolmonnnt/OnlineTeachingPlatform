package com.tsolmon.online_teaching_platform.auth.domain;

public record AuthUser(
        Long id,
        String email,
        Role role
) {
}
