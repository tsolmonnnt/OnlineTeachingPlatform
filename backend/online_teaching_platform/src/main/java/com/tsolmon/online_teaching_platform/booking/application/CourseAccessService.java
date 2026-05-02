package com.tsolmon.online_teaching_platform.booking.application;

import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseAccessService {

    private final BookingRepository bookingRepository;

    /**
     * Student may access course-scoped materials/quizzes only after a confirmed lesson booking for that teacher + course.
     */
    @Transactional(readOnly = true)
    public boolean hasConfirmedAccess(Long studentUserId, Long teacherProfileId, Long courseSubjectId) {
        if (courseSubjectId == null) {
            return false;
        }
        return bookingRepository.existsByStudentUser_IdAndTeacherProfile_IdAndCourseSubject_IdAndStatus(
                studentUserId,
                teacherProfileId,
                courseSubjectId,
                BookingStatus.CONFIRMED
        );
    }
}
