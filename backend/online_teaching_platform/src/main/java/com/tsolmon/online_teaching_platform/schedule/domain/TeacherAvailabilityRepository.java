package com.tsolmon.online_teaching_platform.schedule.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailabilitySlot, Long> {
    List<TeacherAvailabilitySlot> findByTeacherProfile_IdOrderByStartTimeAsc(Long teacherProfileId);

    List<TeacherAvailabilitySlot> findByTeacherProfile_IdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
            Long teacherProfileId,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean existsByTeacherProfile_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long teacherProfileId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );
}

