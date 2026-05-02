package com.tsolmon.online_teaching_platform.admin.api.dto;

import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;

public record AdminTeacherRowResponse(
        Long teacherProfileId,
        Long userId,
        String fullName,
        String email,
        boolean verified
) {
    public static AdminTeacherRowResponse from(TeacherProfile profile) {
        return new AdminTeacherRowResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.isVerified()
        );
    }
}
