package com.tsolmon.online_teaching_platform.teacher.api.dto;

import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record TeacherSummaryResponse(
        Long id,
        Long userId,
        String fullName,
        String headline,
        String avatarUrl,
        List<String> subjects,
        List<String> skills,
        BigDecimal hourlyRate,
        Integer yearsExperience,
        String location,
        boolean verified,
        Double averageRating,
        long reviewCount
) {
    private static List<String> copyList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).toList();
    }

    public static TeacherSummaryResponse from(TeacherProfile profile, Double averageRating, long reviewCount) {
        return new TeacherSummaryResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getHeadline(),
                profile.getAvatarUrl(),
                copyList(profile.getSubjects()),
                copyList(profile.getSkills()),
                profile.getHourlyRate(),
                profile.getYearsExperience(),
                profile.getLocation(),
                profile.isVerified(),
                averageRating,
                reviewCount
        );
    }
}

