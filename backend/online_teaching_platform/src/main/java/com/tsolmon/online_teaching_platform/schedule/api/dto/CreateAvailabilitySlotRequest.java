package com.tsolmon.online_teaching_platform.schedule.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAvailabilitySlotRequest(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
) {
}

