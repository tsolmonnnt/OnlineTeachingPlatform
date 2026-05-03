package com.tsolmon.online_teaching_platform.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentUser_IdOrderByCreatedAtDesc(Long studentId);

    List<Booking> findByTeacherProfile_IdOrderByCreatedAtDesc(Long teacherProfileId);

    List<Booking> findTop5ByTeacherProfile_IdOrderByCreatedAtDesc(Long teacherProfileId);

    List<Booking> findTop5ByStudentUser_IdOrderByCreatedAtDesc(Long studentUserId);

    List<Booking> findByAvailabilitySlot_Id(Long availabilitySlotId);

    long countByTeacherProfile_IdAndStatus(Long teacherProfileId, BookingStatus status);

    long countByStudentUser_IdAndStatus(Long studentUserId, BookingStatus status);

    @Query("""
            SELECT COUNT(b) FROM Booking b JOIN b.availabilitySlot s
            WHERE b.teacherProfile.id = :tid AND b.status = :status
            AND s.startTime >= :from AND s.startTime < :to""")
    long countTeacherConfirmedLessonsSlotBetween(
            @Param("tid") Long teacherProfileId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT COUNT(b) FROM Booking b JOIN b.availabilitySlot s
            WHERE b.studentUser.id = :sid AND b.status = :status
            AND s.startTime >= :from AND s.startTime < :to""")
    long countStudentConfirmedLessonsSlotBetween(
            @Param("sid") Long studentUserId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT COUNT(DISTINCT b.studentUser.id) FROM Booking b
            WHERE b.teacherProfile.id = :tid AND b.status = :status""")
    long countDistinctStudentsForTeacher(
            @Param("tid") Long teacherProfileId,
            @Param("status") BookingStatus status
    );

    boolean existsByStudentUser_IdAndTeacherProfile_IdAndCourseSubject_IdAndStatus(
            Long studentUserId,
            Long teacherProfileId,
            Long courseSubjectId,
            BookingStatus status
    );
}

