package com.tsolmon.online_teaching_platform.course.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.course.api.dto.CourseCategoryResponse;
import com.tsolmon.online_teaching_platform.course.api.dto.CourseSubjectResponse;
import com.tsolmon.online_teaching_platform.course.domain.CourseCategory;
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
    /** Category for subjects created from teacher profile names (optional sync with global catalog). */
    public static final String TEACHER_SUBJECT_CATEGORY_NAME = "Багшын хичээл";

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
     * For each non-empty subject label, ensures a {@link CourseSubject} exists (creates under {@link #TEACHER_SUBJECT_CATEGORY_NAME} if missing).
     * Teachers name subjects freely in their profile; rows appear in the catalog for booking/material/quiz linkage.
     */
    @Transactional
    public void ensureSubjectsExistForStrings(List<String> subjectStrings) {
        if (subjectStrings == null || subjectStrings.isEmpty()) {
            return;
        }
        CourseCategory bucket = ensureTeacherSubjectCategory();
        for (String entry : subjectStrings) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String name = trimSubjectName(entry.trim());
            if (subjectRepository.findByNameIgnoreCase(name).isPresent()) {
                continue;
            }
            CourseSubject cs = new CourseSubject();
            cs.setName(name);
            cs.setDescription(null);
            cs.setCategory(bucket);
            subjectRepository.save(cs);
        }
    }

    /**
     * Subjects for the current teacher: profile labels, each backed by a catalog row (created on demand).
     */
    @Transactional
    public List<CourseSubjectResponse> getTeachingSubjects(AuthUser authUser) {
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        ensureSubjectsExistForStrings(teacher.getSubjects());
        Map<Long, CourseSubject> byId = new LinkedHashMap<>();
        for (String entry : teacher.getSubjects()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = trimSubjectName(entry.trim());
            subjectRepository.findByNameIgnoreCase(trimmed).ifPresent(cs -> byId.putIfAbsent(cs.getId(), cs));
        }
        return byId.values().stream()
                .sorted(Comparator.comparing(CourseSubject::getName, String.CASE_INSENSITIVE_ORDER))
                .map(CourseSubjectResponse::from)
                .toList();
    }

    private CourseCategory ensureTeacherSubjectCategory() {
        return categoryRepository.findByNameIgnoreCase(TEACHER_SUBJECT_CATEGORY_NAME)
                .orElseGet(() -> {
                    CourseCategory c = new CourseCategory();
                    c.setName(TEACHER_SUBJECT_CATEGORY_NAME);
                    c.setDescription("Багшийн оруулсан хичээлийн нэрүүд");
                    return categoryRepository.save(c);
                });
    }

    private static String trimSubjectName(String name) {
        int max = 120;
        return name.length() <= max ? name : name.substring(0, max);
    }
}

