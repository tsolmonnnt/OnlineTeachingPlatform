package com.tsolmon.online_teaching_platform.schedule.domain;

import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "teacher_availability_slots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_slot_range", columnNames = {"teacher_profile_id", "start_time", "end_time"})
})
public class TeacherAvailabilitySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacherProfile;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private boolean booked;

    @PrePersist
    public void prePersist() {
        if (endTime != null && startTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("endTime must be after startTime");
        }
    }
}

