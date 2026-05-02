package com.tsolmon.online_teaching_platform.material.domain;

import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "teaching_materials")
public class TeachingMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacherProfile;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_subject_id")
    private CourseSubject courseSubject;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 120)
    private String cloudinaryPublicId;

    @Column(nullable = false, length = 1024)
    private String secureUrl;

    @Column(length = 120)
    private String contentType;

    private Long sizeBytes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
