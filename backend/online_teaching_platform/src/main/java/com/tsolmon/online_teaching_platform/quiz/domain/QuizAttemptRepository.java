package com.tsolmon.online_teaching_platform.quiz.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuiz_IdAndStudentUser_IdOrderByCompletedAtDesc(Long quizId, Long studentUserId);
}
