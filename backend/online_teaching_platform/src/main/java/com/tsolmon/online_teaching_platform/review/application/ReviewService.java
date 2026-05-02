package com.tsolmon.online_teaching_platform.review.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.review.api.dto.CreateReviewRequest;
import com.tsolmon.online_teaching_platform.review.api.dto.ReviewResponse;
import com.tsolmon.online_teaching_platform.review.domain.Review;
import com.tsolmon.online_teaching_platform.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForTeacher(Long teacherProfileId) {
        return reviewRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(teacherProfileId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse create(AuthUser authUser, CreateReviewRequest request) {
        if (authUser.role() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can leave reviews");
        }

        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getStudentUser().getId().equals(authUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can only review confirmed bookings");
        }

        if (reviewRepository.existsByBooking_Id(booking.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking was already reviewed");
        }

        Review review = new Review();
        review.setStudentUser(booking.getStudentUser());
        review.setTeacherProfile(booking.getTeacherProfile());
        review.setBooking(booking);
        review.setRating(request.rating());
        review.setComment(request.comment() != null ? request.comment().trim() : null);

        Review saved = reviewRepository.save(review);
        return ReviewResponse.from(saved);
    }
}
