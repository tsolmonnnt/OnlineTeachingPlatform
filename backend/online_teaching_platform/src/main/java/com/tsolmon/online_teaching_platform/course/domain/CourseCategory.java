package com.tsolmon.online_teaching_platform.course.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "course_categories")
public class CourseCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;
}

