package com.tsolmon.online_teaching_platform.course.api.dto;

import com.tsolmon.online_teaching_platform.course.domain.CourseCategory;

public record CourseCategoryResponse(
        Long id,
        String name,
        String description
) {
    public static CourseCategoryResponse from(CourseCategory category) {
        return new CourseCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}

