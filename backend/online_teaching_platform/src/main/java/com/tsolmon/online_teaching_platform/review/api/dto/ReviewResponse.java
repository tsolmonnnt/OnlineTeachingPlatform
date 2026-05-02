package com.tsolmon.online_teaching_platform.review.api.dto;

import com.tsolmon.online_teaching_platform.review.domain.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookingId,
        String studentName,
        int rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getStudentUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
