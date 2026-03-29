package com.tsolmon.online_teaching_platform.course.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "course_subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_subject_name", columnNames = "name")
})
public class CourseSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CourseCategory category;
}

