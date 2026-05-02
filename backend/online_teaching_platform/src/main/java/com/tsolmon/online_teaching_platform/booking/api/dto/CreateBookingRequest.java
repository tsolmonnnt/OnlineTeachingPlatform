package com.tsolmon.online_teaching_platform.booking.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull Long teacherId,
        @NotNull Long slotId,
        /** Optional; if omitted, the course name from the slot is used. */
        @Size(max = 120) String subject,
        @Size(max = 1000) String note
) {
}
