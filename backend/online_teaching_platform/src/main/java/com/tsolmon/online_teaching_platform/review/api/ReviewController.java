package com.tsolmon.online_teaching_platform.review.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.review.api.dto.CreateReviewRequest;
import com.tsolmon.online_teaching_platform.review.api.dto.ReviewResponse;
import com.tsolmon.online_teaching_platform.review.application.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/teacher/{teacherProfileId}")
    public List<ReviewResponse> listForTeacher(@PathVariable Long teacherProfileId) {
        return reviewService.listForTeacher(teacherProfileId);
    }

    @PostMapping
    public ReviewResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return reviewService.create(authUser, request);
    }
}
