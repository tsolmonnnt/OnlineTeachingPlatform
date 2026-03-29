package com.tsolmon.online_teaching_platform.course.api;

import com.tsolmon.online_teaching_platform.course.api.dto.CourseCategoryResponse;
import com.tsolmon.online_teaching_platform.course.api.dto.CourseSubjectResponse;
import com.tsolmon.online_teaching_platform.course.application.CourseCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseCatalogService courseCatalogService;

    @GetMapping("/categories")
    public List<CourseCategoryResponse> categories() {
        return courseCatalogService.getCategories();
    }

    @GetMapping("/subjects")
    public List<CourseSubjectResponse> subjects(@RequestParam(required = false) Long categoryId) {
        return courseCatalogService.getSubjects(categoryId);
    }
}

