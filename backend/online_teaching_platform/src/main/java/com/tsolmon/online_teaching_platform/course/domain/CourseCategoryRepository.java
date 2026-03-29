package com.tsolmon.online_teaching_platform.course.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    Optional<CourseCategory> findByNameIgnoreCase(String name);
}

