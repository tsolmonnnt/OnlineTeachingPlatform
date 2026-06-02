package com.tsolmon.online_teaching_platform.booking.application;

import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseAccessService {

    /** Materials/quizzes stay accessible for this many days after the most recent attended lesson. */
    private static final long ACCESS_WINDOW_DAYS = 30;

    private final BookingRepository bookingRepository;

    /**
     * Student may access course-scoped materials/quizzes only if they have a non-cancelled lesson for this
     * teacher + course that has already started, within a trailing window after it ended. Future bookings do
     * not grant or extend access, so booking far-out slots cannot be used to keep materials open indefinitely.
     */
    @Transactional(readOnly = true)
    public boolean hasCourseAccess(Long studentUserId, Long teacherProfileId, Long courseSubjectId, LocalDateTime now) {
        if (courseSubjectId == null) {
            return false;
        }
        LocalDateTime windowStart = now.minusDays(ACCESS_WINDOW_DAYS);
        return bookingRepository.hasStartedLessonWithinWindow(
                studentUserId,
                teacherProfileId,
                courseSubjectId,
                BookingStatus.CANCELLED,
                now,
                windowStart
        );
    }
}
