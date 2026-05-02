package com.tsolmon.online_teaching_platform.quiz.api.dto;

import com.tsolmon.online_teaching_platform.quiz.domain.Quiz;

import java.time.LocalDateTime;

public record QuizSummaryResponse(
        Long id,
        Long courseSubjectId,
        String courseSubjectName,
        String title,
        String description,
        int timeLimitMinutes,
        boolean published,
        int questionCount,
        LocalDateTime createdAt
) {
    public static QuizSummaryResponse from(Quiz q) {
        Long csId = q.getCourseSubject() != null ? q.getCourseSubject().getId() : null;
        String csName = q.getCourseSubject() != null ? q.getCourseSubject().getName() : null;
        return new QuizSummaryResponse(
                q.getId(),
                csId,
                csName,
                q.getTitle(),
                q.getDescription(),
                q.getTimeLimitMinutes(),
                q.isPublished(),
                q.getQuestions().size(),
                q.getCreatedAt()
        );
    }
}
