package com.tsolmon.online_teaching_platform.schedule.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * One slot = one 30-minute lesson. End time is computed on the server as start + 30 minutes.
 */
public record CreateAvailabilitySlotRequest(
        @NotNull LocalDateTime startTime,
        @NotNull Long courseSubjectId
) {
}
