package com.tsolmon.online_teaching_platform.schedule.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailabilitySlot, Long> {
    List<TeacherAvailabilitySlot> findByTeacherProfile_IdOrderByStartTimeAsc(Long teacherProfileId);

    /**
     * Slots that overlap the half-open range {@code [from, to)} (any intersection).
     */
    @Query("""
            SELECT s FROM TeacherAvailabilitySlot s
            WHERE s.teacherProfile.id = :teacherId
            AND s.endTime > :from
            AND s.startTime < :to
            ORDER BY s.startTime ASC
            """)
    List<TeacherAvailabilitySlot> findOverlappingTeacherSchedule(
            @Param("teacherId") Long teacherId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    boolean existsByTeacherProfile_IdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long teacherProfileId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    /**
     * Same overlap semantics as {@link #existsByTeacherProfile_IdAndStartTimeLessThanAndEndTimeGreaterThan},
     * but ignores the slot with {@code excludeSlotId} (for updates).
     */
    boolean existsByTeacherProfile_IdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long teacherProfileId,
            Long excludeSlotId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );
}

