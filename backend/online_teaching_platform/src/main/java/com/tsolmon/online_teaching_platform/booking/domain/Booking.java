package com.tsolmon.online_teaching_platform.booking.domain;

import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacherProfile;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_slot_id", nullable = false, unique = true)
    private TeacherAvailabilitySlot availabilitySlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_subject_id")
    private CourseSubject courseSubject;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

