package com.tsolmon.online_teaching_platform.teacher.api.dto;

import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TeacherProfileResponse(
        Long id,
        Long userId,
        String headline,
        String bio,
        List<String> subjects,
        List<String> skills,
        String avatarUrl,
        BigDecimal hourlyRate,
        List<String> languages,
        String location,
        String phone,
        Integer yearsExperience,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TeacherProfileResponse from(TeacherProfile profile) {
        return new TeacherProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getSubjects(),
                profile.getSkills(),
                profile.getAvatarUrl(),
                profile.getHourlyRate(),
                profile.getLanguages(),
                profile.getLocation(),
                profile.getPhone(),
                profile.getYearsExperience(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
