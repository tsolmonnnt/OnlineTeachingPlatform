package com.tsolmon.online_teaching_platform.booking.api.dto;

import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.booking.domain.BookingType;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        BookingStatus status,
        BookingType bookingType,
        String subject,
        String note,
        String meetingLink,
        boolean reminderSent,
        Long parentBookingId,
        Integer packageTotalLessons,
        Integer packageCompletedLessons,
        Integer packageBookedLessons,
        Long studentUserId,
        String studentName,
        Long teacherId,
        String teacherName,
        Long slotId,
        LocalDateTime slotStartTime,
        LocalDateTime slotEndTime,
        Long courseSubjectId,
        String courseSubjectName,
        boolean canReview,
        LocalDateTime reviewDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BookingResponse from(Booking booking) {
        return from(booking, false, null, null);
    }

    public static BookingResponse from(
            Booking booking,
            boolean canReview,
            LocalDateTime reviewDeadline
    ) {
        return from(booking, canReview, reviewDeadline, null);
    }

    public static BookingResponse from(
            Booking booking,
            boolean canReview,
            LocalDateTime reviewDeadline,
            Integer packageBookedLessons
    ) {
        Long csId = booking.getCourseSubject() != null ? booking.getCourseSubject().getId() : null;
        String csName = booking.getCourseSubject() != null ? booking.getCourseSubject().getName() : null;
        Booking packageRoot = resolvePackageRoot(booking);
        Long parentId = booking.getParentBooking() != null ? booking.getParentBooking().getId() : null;
        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getBookingType(),
                booking.getSubject(),
                booking.getNote(),
                booking.getMeetingLink(),
                booking.isReminderSent(),
                parentId,
                packageRoot.getPackageTotalLessons(),
                packageRoot.getPackageCompletedLessons(),
                packageBookedLessons,
                booking.getStudentUser().getId(),
                booking.getStudentUser().getFullName(),
                booking.getTeacherProfile().getId(),
                booking.getTeacherProfile().getUser().getFullName(),
                booking.getAvailabilitySlot().getId(),
                booking.getAvailabilitySlot().getStartTime(),
                booking.getAvailabilitySlot().getEndTime(),
                csId,
                csName,
                canReview,
                reviewDeadline,
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private static Booking resolvePackageRoot(Booking booking) {
        if (booking.getParentBooking() != null) {
            return booking.getParentBooking();
        }
        return booking;
    }
}
