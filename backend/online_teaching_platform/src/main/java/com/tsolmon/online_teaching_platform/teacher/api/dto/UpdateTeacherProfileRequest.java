package com.tsolmon.online_teaching_platform.teacher.api.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateTeacherProfileRequest(
        @Size(max = 120) String headline,
        @Size(max = 2000) String bio,
        List<String> subjects,
        List<String> skills,
        String avatarUrl,
        BigDecimal hourlyRate,
        List<String> languages,
        String location,
        @Size(max = 40) String phone,
        Integer yearsExperience
) {
}
