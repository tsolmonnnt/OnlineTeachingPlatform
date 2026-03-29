package com.tsolmon.online_teaching_platform.course.application;

import com.tsolmon.online_teaching_platform.course.api.dto.CourseCategoryResponse;
import com.tsolmon.online_teaching_platform.course.api.dto.CourseSubjectResponse;
import com.tsolmon.online_teaching_platform.course.domain.CourseCategoryRepository;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseCatalogService {
    private final CourseCategoryRepository categoryRepository;
    private final CourseSubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<CourseCategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(CourseCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseSubjectResponse> getSubjects(Long categoryId) {
        if (categoryId == null) {
            return subjectRepository.findAll().stream()
                    .map(CourseSubjectResponse::from)
                    .toList();
        }
        return subjectRepository.findByCategory_Id(categoryId).stream()
                .map(CourseSubjectResponse::from)
                .toList();
    }
}

