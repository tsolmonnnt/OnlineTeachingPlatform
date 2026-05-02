package com.tsolmon.online_teaching_platform.quiz.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestionType questionType;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Column(length = 4000)
    private String optionsJson;

    @Column(nullable = false, length = 2000)
    private String correctAnswer;
}
