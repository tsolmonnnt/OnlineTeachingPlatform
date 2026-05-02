package com.tsolmon.online_teaching_platform.quiz.domain;

import com.tsolmon.online_teaching_platform.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false, length = 8000)
    private String submittedAnswersJson;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.completedAt = LocalDateTime.now();
    }
}
