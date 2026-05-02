package com.tsolmon.online_teaching_platform.quiz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateQuizRequest(
        @NotNull Long courseSubjectId,
        @NotBlank String title,
        String description,
        @NotNull @Min(1) Integer timeLimitMinutes,
        @NotEmpty @Valid List<CreateQuestionRequest> questions
) {
}
