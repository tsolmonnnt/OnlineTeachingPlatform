package com.tsolmon.online_teaching_platform.schedule.api.dto;

import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;

import java.time.LocalDateTime;

public record AvailabilitySlotResponse(
        Long id,
        Long teacherProfileId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean booked,
        Long courseSubjectId,
        String courseSubjectName
) {
    public static AvailabilitySlotResponse from(TeacherAvailabilitySlot slot) {
        Long csId = slot.getCourseSubject() != null ? slot.getCourseSubject().getId() : null;
        String csName = slot.getCourseSubject() != null ? slot.getCourseSubject().getName() : null;
        return new AvailabilitySlotResponse(
                slot.getId(),
                slot.getTeacherProfile().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.isBooked(),
                csId,
                csName
        );
    }
}
