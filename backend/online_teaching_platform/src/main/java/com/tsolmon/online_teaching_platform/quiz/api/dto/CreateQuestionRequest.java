package com.tsolmon.online_teaching_platform.quiz.api.dto;

import com.tsolmon.online_teaching_platform.quiz.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String prompt,
        String optionsJson,
        @NotBlank String correctAnswer
) {
}
