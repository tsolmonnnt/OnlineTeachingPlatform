package com.tsolmon.online_teaching_platform.teacher.api.dto;

import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;

import java.math.BigDecimal;
import java.util.List;

public record TeacherDetailResponse(
        Long id,
        Long userId,
        String fullName,
        String headline,
        String bio,
        List<String> subjects,
        List<String> skills,
        String avatarUrl,
        BigDecimal hourlyRate,
        List<String> languages,
        String location,
        String phone,
        Integer yearsExperience
) {
    public static TeacherDetailResponse from(TeacherProfile profile) {
        return new TeacherDetailResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getSubjects(),
                profile.getSkills(),
                profile.getAvatarUrl(),
                profile.getHourlyRate(),
                profile.getLanguages(),
                profile.getLocation(),
                profile.getPhone(),
                profile.getYearsExperience()
        );
    }
}

