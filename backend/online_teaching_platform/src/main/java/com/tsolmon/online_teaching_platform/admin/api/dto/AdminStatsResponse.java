package com.tsolmon.online_teaching_platform.admin.api.dto;

public record AdminStatsResponse(
        long totalUsers,
        long studentCount,
        long teacherCount,
        long adminCount,
        long totalBookings,
        long verifiedTeacherCount,
        long totalTeachers
) {
}
