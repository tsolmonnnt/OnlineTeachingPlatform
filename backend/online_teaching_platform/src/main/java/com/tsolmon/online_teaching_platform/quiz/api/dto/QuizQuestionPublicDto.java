package com.tsolmon.online_teaching_platform.quiz.api.dto;

import com.tsolmon.online_teaching_platform.quiz.domain.QuestionType;

public record QuizQuestionPublicDto(
        Long id,
        int orderIndex,
        QuestionType questionType,
        String prompt,
        String optionsJson
) {
}
