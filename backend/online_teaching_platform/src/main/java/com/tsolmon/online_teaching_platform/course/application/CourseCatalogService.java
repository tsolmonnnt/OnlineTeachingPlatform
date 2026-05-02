package com.tsolmon.online_teaching_platform.course.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.course.api.dto.CourseCategoryResponse;
import com.tsolmon.online_teaching_platform.course.api.dto.CourseSubjectResponse;
import com.tsolmon.online_teaching_platform.course.domain.CourseCategoryRepository;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseCatalogService {
    private final CourseCategoryRepository categoryRepository;
    private final CourseSubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

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

    /**
     * Catalog subjects that match the current teacher's profile subject list (same name matching as schedule/material/quiz validation).
     */
    @Transactional(readOnly = true)
    public List<CourseSubjectResponse> getTeachingSubjects(AuthUser authUser) {
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        Map<Long, CourseSubject> byId = new LinkedHashMap<>();
        for (String entry : teacher.getSubjects()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            subjectRepository.findByNameIgnoreCase(trimmed).ifPresent(cs -> byId.putIfAbsent(cs.getId(), cs));
        }
        return byId.values().stream()
                .sorted(Comparator.comparing(CourseSubject::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CourseSubjectResponse::from)
                .toList();
    }
}

