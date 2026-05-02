package com.tsolmon.online_teaching_platform.teacher.domain;

import com.tsolmon.online_teaching_platform.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 120)
    private String headline;

    @Column(length = 2000)
    private String bio;

    @ElementCollection
    @CollectionTable(name = "teacher_profile_subjects", joinColumns = @JoinColumn(name = "teacher_profile_id"))
    @Column(name = "subject", nullable = false)
    private List<String> subjects = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "teacher_profile_skills", joinColumns = @JoinColumn(name = "teacher_profile_id"))
    @Column(name = "skill", nullable = false)
    private List<String> skills = new ArrayList<>();

    private String avatarUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @ElementCollection
    @CollectionTable(name = "teacher_profile_languages", joinColumns = @JoinColumn(name = "teacher_profile_id"))
    @Column(name = "language", nullable = false)
    private List<String> languages = new ArrayList<>();

    private String location;

    @Column(length = 40)
    private String phone;

    private Integer yearsExperience;

    @Column(nullable = false)
    private boolean verified;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.verified = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
