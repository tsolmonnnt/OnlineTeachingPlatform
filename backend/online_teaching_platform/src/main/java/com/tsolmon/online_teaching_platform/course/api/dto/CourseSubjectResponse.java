package com.tsolmon.online_teaching_platform.course.api.dto;

import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;

public record CourseSubjectResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName
) {
    public static CourseSubjectResponse from(CourseSubject subject) {
        return new CourseSubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getDescription(),
                subject.getCategory().getId(),
                subject.getCategory().getName()
        );
    }
}

