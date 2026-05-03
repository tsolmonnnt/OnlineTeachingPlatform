package com.tsolmon.online_teaching_platform.dashboard.api.dto;

public record PublicPlatformStatsResponse(
        long verifiedTeacherCount,
        long totalBookings,
        long studentCount,
        long totalTeachers
) {
}
