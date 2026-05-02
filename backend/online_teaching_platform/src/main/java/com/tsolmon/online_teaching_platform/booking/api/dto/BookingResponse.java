package com.tsolmon.online_teaching_platform.booking.api.dto;

import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        BookingStatus status,
        String subject,
        String note,
        Long studentUserId,
        String studentName,
        Long teacherId,
        String teacherName,
        Long slotId,
        LocalDateTime slotStartTime,
        LocalDateTime slotEndTime,
        Long courseSubjectId,
        String courseSubjectName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BookingResponse from(Booking booking) {
        Long csId = booking.getCourseSubject() != null ? booking.getCourseSubject().getId() : null;
        String csName = booking.getCourseSubject() != null ? booking.getCourseSubject().getName() : null;
        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getSubject(),
                booking.getNote(),
                booking.getStudentUser().getId(),
                booking.getStudentUser().getFullName(),
                booking.getTeacherProfile().getId(),
                booking.getTeacherProfile().getUser().getFullName(),
                booking.getAvailabilitySlot().getId(),
                booking.getAvailabilitySlot().getStartTime(),
                booking.getAvailabilitySlot().getEndTime(),
                csId,
                csName,
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
