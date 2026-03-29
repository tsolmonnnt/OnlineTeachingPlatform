package com.tsolmon.online_teaching_platform.user.dto;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {
}

