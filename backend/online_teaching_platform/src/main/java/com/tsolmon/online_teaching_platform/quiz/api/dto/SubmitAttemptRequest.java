package com.tsolmon.online_teaching_platform.quiz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAttemptRequest(@NotEmpty @Valid List<AnswerItem> answers) {
    public record AnswerItem(@NotNull Long questionId, String answer) {
    }
}
