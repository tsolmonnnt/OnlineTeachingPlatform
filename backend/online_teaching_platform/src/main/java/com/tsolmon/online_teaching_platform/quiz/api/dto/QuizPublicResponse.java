package com.tsolmon.online_teaching_platform.quiz.api.dto;

import java.util.List;

public record QuizPublicResponse(
        Long id,
        Long teacherProfileId,
        String title,
        String description,
        int timeLimitMinutes,
        List<QuizQuestionPublicDto> questions
) {
}
