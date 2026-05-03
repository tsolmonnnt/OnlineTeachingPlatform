package com.tsolmon.online_teaching_platform.dashboard.api.dto;

import com.tsolmon.online_teaching_platform.booking.api.dto.BookingResponse;

import java.util.List;

public record TeacherDashboardResponse(
        long confirmedLessonsToday,
        long confirmedLessonsTomorrow,
        long pendingBookingsAsTeacher,
        Double averageRating,
        long reviewCount,
        long unreadNotifications,
        long uniqueStudentsConfirmed,
        List<BookingResponse> recentBookings
) {
}
