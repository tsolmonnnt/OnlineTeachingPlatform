package com.tsolmon.online_teaching_platform.booking.api.dto;

import com.tsolmon.online_teaching_platform.booking.domain.BookingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull Long teacherId,
        @NotNull Long slotId,
        /** Optional; if omitted, the course name from the slot is used. */
        @Size(max = 120) String subject,
        @Size(max = 1000) String note,
        BookingType bookingType,
        Long parentBookingId,
        @Min(1) Integer packageTotalLessons
) {
}
