package com.tsolmon.online_teaching_platform.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTeacherProfile_IdOrderByCreatedAtDesc(Long teacherProfileId);

    boolean existsByBooking_Id(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.teacherProfile.id = :teacherId")
    Optional<Double> averageRatingByTeacher(@Param("teacherId") Long teacherProfileId);

    long countByTeacherProfile_Id(Long teacherProfileId);
}
