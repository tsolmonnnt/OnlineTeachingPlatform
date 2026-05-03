package com.tsolmon.online_teaching_platform.dashboard.api.dto;

import com.tsolmon.online_teaching_platform.booking.api.dto.BookingResponse;

import java.util.List;

public record StudentDashboardResponse(
        long upcomingConfirmedLessons,
        long pendingBookingsAsStudent,
        long unreadNotifications,
        List<BookingResponse> recentBookings
) {
}
