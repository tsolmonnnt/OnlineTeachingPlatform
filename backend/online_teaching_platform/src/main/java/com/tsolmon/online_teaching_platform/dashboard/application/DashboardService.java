package com.tsolmon.online_teaching_platform.dashboard.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.api.dto.BookingResponse;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.dashboard.api.dto.StudentDashboardResponse;
import com.tsolmon.online_teaching_platform.dashboard.api.dto.TeacherDashboardResponse;
import com.tsolmon.online_teaching_platform.notification.domain.NotificationRepository;
import com.tsolmon.online_teaching_platform.review.domain.ReviewRepository;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final int UPCOMING_STUDENT_DAYS = 21;

    private final TeacherRepository teacherRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(AuthUser authUser) {
        if (authUser.role() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher only");
        }
        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        Long tid = teacher.getId();

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime tomorrowEnd = today.plusDays(2).atStartOfDay();

        long todayCount = bookingRepository.countTeacherConfirmedLessonsSlotBetween(
                tid, BookingStatus.CONFIRMED, dayStart, dayEnd);
        long tomorrowCount = bookingRepository.countTeacherConfirmedLessonsSlotBetween(
                tid, BookingStatus.CONFIRMED, tomorrowStart, tomorrowEnd);
        long pending = bookingRepository.countByTeacherProfile_IdAndStatus(tid, BookingStatus.PENDING);

        Double avg = reviewRepository.averageRatingByTeacher(tid).orElse(null);
        long reviewCount = reviewRepository.countByTeacherProfile_Id(tid);
        long unread = notificationRepository.countByRecipientUser_IdAndIsReadFalse(authUser.id());
        long uniqueStudents = bookingRepository.countDistinctStudentsForTeacher(tid, BookingStatus.CONFIRMED);

        List<BookingResponse> recent = bookingRepository.findTop5ByTeacherProfile_IdOrderByCreatedAtDesc(tid)
                .stream()
                .map(BookingResponse::from)
                .toList();

        return new TeacherDashboardResponse(
                todayCount,
                tomorrowCount,
                pending,
                avg,
                reviewCount,
                unread,
                uniqueStudents,
                recent
        );
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse getStudentDashboard(AuthUser authUser) {
        if (authUser.role() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student only");
        }
        Long sid = authUser.id();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(UPCOMING_STUDENT_DAYS);

        long upcoming = bookingRepository.countStudentConfirmedLessonsSlotBetween(
                sid, BookingStatus.CONFIRMED, now, horizon);
        long pending = bookingRepository.countByStudentUser_IdAndStatus(sid, BookingStatus.PENDING);
        long unread = notificationRepository.countByRecipientUser_IdAndIsReadFalse(sid);

        List<BookingResponse> recent = bookingRepository.findTop5ByStudentUser_IdOrderByCreatedAtDesc(sid)
                .stream()
                .map(BookingResponse::from)
                .toList();

        return new StudentDashboardResponse(upcoming, pending, unread, recent);
    }
}
