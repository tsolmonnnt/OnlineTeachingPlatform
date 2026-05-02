package com.tsolmon.online_teaching_platform.quiz.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.quiz.api.dto.*;
import com.tsolmon.online_teaching_platform.quiz.application.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;

    @PostMapping
    public QuizSummaryResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateQuizRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return quizService.create(authUser, request);
    }

    @GetMapping("/mine")
    public List<QuizSummaryResponse> listMine(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return quizService.listMine(authUser);
    }

    @GetMapping("/mine/{quizId}")
    public QuizTeacherDetailResponse getMineDetail(
            Authentication authentication,
            @PathVariable Long quizId
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return quizService.getTeacherQuizDetail(authUser, quizId);
    }

    @DeleteMapping("/mine/{quizId}")
    public void delete(Authentication authentication, @PathVariable Long quizId) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        quizService.delete(authUser, quizId);
    }

    @GetMapping("/teacher/{teacherProfileId}/published")
    public List<QuizSummaryResponse> listPublishedForTeacher(@PathVariable Long teacherProfileId) {
        return quizService.listPublishedForTeacher(teacherProfileId);
    }

    @GetMapping("/{quizId}/public")
    public QuizPublicResponse getPublishedQuiz(
            Authentication authentication,
            @PathVariable Long quizId
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return quizService.getPublishedQuiz(quizId, authUser);
    }

    @PostMapping("/{quizId}/attempts")
    public AttemptResultResponse submitAttempt(
            Authentication authentication,
            @PathVariable Long quizId,
            @Valid @RequestBody SubmitAttemptRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return quizService.submitAttempt(authUser, quizId, request);
    }
}
