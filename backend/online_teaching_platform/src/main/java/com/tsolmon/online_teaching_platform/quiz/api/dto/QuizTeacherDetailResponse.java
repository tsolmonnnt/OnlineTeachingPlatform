package com.tsolmon.online_teaching_platform.quiz.api.dto;

import com.tsolmon.online_teaching_platform.quiz.domain.QuestionType;

import java.time.LocalDateTime;
import java.util.List;

public record QuizTeacherDetailResponse(
        Long id,
        String title,
        String description,
        int timeLimitMinutes,
        boolean published,
        LocalDateTime createdAt,
        List<QuizQuestionTeacherDto> questions
) {
    public record QuizQuestionTeacherDto(
            Long id,
            int orderIndex,
            QuestionType questionType,
            String prompt,
            String optionsJson,
            String correctAnswer
    ) {
    }
}
