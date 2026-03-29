package com.tsolmon.online_teaching_platform.schedule.api.dto;

import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;

import java.time.LocalDateTime;

public record AvailabilitySlotResponse(
        Long id,
        Long teacherProfileId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean booked
) {
    public static AvailabilitySlotResponse from(TeacherAvailabilitySlot slot) {
        return new AvailabilitySlotResponse(
                slot.getId(),
                slot.getTeacherProfile().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.isBooked()
        );
    }
}

