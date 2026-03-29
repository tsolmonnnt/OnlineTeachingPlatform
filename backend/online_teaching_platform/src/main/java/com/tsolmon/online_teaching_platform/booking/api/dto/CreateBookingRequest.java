package com.tsolmon.online_teaching_platform.booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull Long teacherId,
        @NotNull Long slotId,
        @NotBlank @Size(max = 120) String subject,
        @Size(max = 1000) String note
) {
}

