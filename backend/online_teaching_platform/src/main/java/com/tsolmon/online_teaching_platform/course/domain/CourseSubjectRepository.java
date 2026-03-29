package com.tsolmon.online_teaching_platform.course.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseSubjectRepository extends JpaRepository<CourseSubject, Long> {
    Optional<CourseSubject> findByNameIgnoreCase(String name);
    List<CourseSubject> findByCategory_Id(Long categoryId);
}

