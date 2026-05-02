package com.tsolmon.online_teaching_platform.review.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull Long bookingId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String comment
) {
}
