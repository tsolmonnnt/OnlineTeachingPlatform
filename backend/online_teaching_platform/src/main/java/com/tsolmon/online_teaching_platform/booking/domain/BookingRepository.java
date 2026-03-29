package com.tsolmon.online_teaching_platform.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStudentUser_IdOrderByCreatedAtDesc(Long studentId);
    List<Booking> findByTeacherProfile_IdOrderByCreatedAtDesc(Long teacherProfileId);
}

