package com.tsolmon.online_teaching_platform.teacher.api.dto;

import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
        boolean verified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).toList();
    }

    public static TeacherProfileResponse from(TeacherProfile profile) {
        return new TeacherProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getHeadline(),
                profile.getBio(),
                copyList(profile.getSubjects()),
                copyList(profile.getSkills()),
                profile.getAvatarUrl(),
                profile.getHourlyRate(),
                copyList(profile.getLanguages()),
                profile.getLocation(),
                profile.getPhone(),
                profile.getYearsExperience(),
                profile.isVerified(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
