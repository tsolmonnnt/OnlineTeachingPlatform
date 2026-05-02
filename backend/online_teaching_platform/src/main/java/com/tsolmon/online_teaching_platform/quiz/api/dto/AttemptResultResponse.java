package com.tsolmon.online_teaching_platform.quiz.api.dto;

public record AttemptResultResponse(
        Long attemptId,
        Long quizId,
        int score,
        int maxScore,
        int percent
) {
}
