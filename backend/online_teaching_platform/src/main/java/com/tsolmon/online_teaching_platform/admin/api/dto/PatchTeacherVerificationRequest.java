package com.tsolmon.online_teaching_platform.admin.api.dto;

import jakarta.validation.constraints.NotNull;

public record PatchTeacherVerificationRequest(@NotNull Boolean verified) {
}
