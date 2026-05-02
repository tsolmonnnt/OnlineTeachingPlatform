package com.tsolmon.online_teaching_platform.quiz.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByTeacherProfile_IdOrderByCreatedAtDesc(Long teacherProfileId);

    List<Quiz> findByTeacherProfile_IdAndPublishedIsTrueOrderByCreatedAtDesc(Long teacherProfileId);
}
