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

    long countByParentBooking_IdAndStatusNot(Long parentBookingId, BookingStatus status);

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

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b
            JOIN b.availabilitySlot s
            WHERE b.studentUser.id = :sid AND b.teacherProfile.id = :tid
              AND b.courseSubject.id = :cid AND b.status <> :cancelled
              AND s.startTime <= :now AND s.endTime >= :windowStart""")
    boolean hasStartedLessonWithinWindow(
            @Param("sid") Long studentUserId,
            @Param("tid") Long teacherProfileId,
            @Param("cid") Long courseSubjectId,
            @Param("cancelled") BookingStatus cancelled,
            @Param("now") LocalDateTime now,
            @Param("windowStart") LocalDateTime windowStart
    );

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.availabilitySlot s
            WHERE b.status = :status
            AND s.startTime <= :now
            """)
    List<Booking> findReadyToStart(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.availabilitySlot s
            WHERE b.status IN :statuses
            AND s.endTime <= :now
            """)
    List<Booking> findReadyToComplete(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.availabilitySlot s
            WHERE b.status = :status
            AND b.reminderSent = false
            AND s.startTime > :now
            AND s.startTime <= :limit
            """)
    List<Booking> findReminderCandidates(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now,
            @Param("limit") LocalDateTime limit
    );
}

