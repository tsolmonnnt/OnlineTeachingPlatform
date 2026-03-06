package com.tsolmon.online_teaching_platform.teacher.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByUser_Id(Long userId);
}
