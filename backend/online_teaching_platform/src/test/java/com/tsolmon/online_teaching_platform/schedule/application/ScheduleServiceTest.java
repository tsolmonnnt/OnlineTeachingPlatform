package com.tsolmon.online_teaching_platform.schedule.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.schedule.api.dto.CreateAvailabilitySlotRequest;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilityRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private TeacherAvailabilityRepository availabilityRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CourseSubjectRepository courseSubjectRepository;

    @Test
    void addMySlotRejectsUnknownUnofferedAndOverlappingSubjects() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(courseSubjectRepository.findById(99L)).thenReturn(Optional.empty());
        assertStatus(400, () -> service().addMySlot(authUser, request(start, 99L)));

        when(courseSubjectRepository.findById(2L)).thenReturn(Optional.of(subject(2L, "Python")));
        assertStatus(400, () -> service().addMySlot(authUser, request(start, 2L)));

        when(courseSubjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Java")));
        when(availabilityRepository.existsByTeacherProfile_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                10L,
                start.plusMinutes(30),
                start
        )).thenReturn(true);
        assertStatus(409, () -> service().addMySlot(authUser, request(start, 1L)));
    }

    @Test
    void addMySlotSavesAvailableSlotWhenRulesPass() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");
        CourseSubject java = subject(1L, "Java");
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(courseSubjectRepository.findById(1L)).thenReturn(Optional.of(java));
        when(availabilityRepository.save(org.mockito.ArgumentMatchers.any(TeacherAvailabilitySlot.class)))
                .thenAnswer(invocation -> {
                    TeacherAvailabilitySlot slot = invocation.getArgument(0);
                    slot.setId(50L);
                    return slot;
                });

        assertThat(service().addMySlot(authUser, request(start, 1L)).id()).isEqualTo(50L);
    }

    @Test
    void updateMySlotRejectsOwnershipBookedSubjectAndOverlapFailures() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");
        TeacherProfile otherTeacher = teacher(11L, 2L, "Java");
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(availabilityRepository.findById(404L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service().updateMySlot(authUser, 404L, request(start, 1L)));

        when(availabilityRepository.findById(20L)).thenReturn(Optional.of(slot(20L, otherTeacher, start, false)));
        assertStatus(403, () -> service().updateMySlot(authUser, 20L, request(start, 1L)));

        when(availabilityRepository.findById(21L)).thenReturn(Optional.of(slot(21L, teacher, start, true)));
        assertStatus(409, () -> service().updateMySlot(authUser, 21L, request(start, 1L)));

        when(availabilityRepository.findById(22L)).thenReturn(Optional.of(slot(22L, teacher, start, false)));
        when(courseSubjectRepository.findById(99L)).thenReturn(Optional.empty());
        assertStatus(400, () -> service().updateMySlot(authUser, 22L, request(start, 99L)));

        when(courseSubjectRepository.findById(2L)).thenReturn(Optional.of(subject(2L, "Python")));
        assertStatus(400, () -> service().updateMySlot(authUser, 22L, request(start, 2L)));

        when(courseSubjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "Java")));
        when(availabilityRepository.existsByTeacherProfile_IdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                10L,
                22L,
                start.plusMinutes(30),
                start
        )).thenReturn(true);
        assertStatus(409, () -> service().updateMySlot(authUser, 22L, request(start, 1L)));
    }

    @Test
    void deleteMySlotRejectsForeignBookedAndActiveBookingsButDeletesCancelledBookings() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");
        TeacherProfile otherTeacher = teacher(11L, 2L, "Java");
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(availabilityRepository.findById(404L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service().deleteMySlot(authUser, 404L));

        when(availabilityRepository.findById(20L)).thenReturn(Optional.of(slot(20L, otherTeacher, start, false)));
        assertStatus(403, () -> service().deleteMySlot(authUser, 20L));

        when(availabilityRepository.findById(21L)).thenReturn(Optional.of(slot(21L, teacher, start, true)));
        assertStatus(409, () -> service().deleteMySlot(authUser, 21L));

        TeacherAvailabilitySlot slot = slot(22L, teacher, start, false);
        when(availabilityRepository.findById(22L)).thenReturn(Optional.of(slot));
        when(bookingRepository.findByAvailabilitySlot_Id(22L))
                .thenReturn(List.of(booking(BookingStatus.PENDING)))
                .thenReturn(List.of(booking(BookingStatus.CANCELLED)));

        assertStatus(409, () -> service().deleteMySlot(authUser, 22L));

        service().deleteMySlot(authUser, 22L);
        verify(bookingRepository).deleteAll(List.of(booking(BookingStatus.CANCELLED)));
        verify(availabilityRepository).delete(slot);
    }

    @Test
    void getTeacherScheduleValidatesRangeAndTeacherExistence() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime to = from.plusHours(1);

        assertStatus(400, () -> service().getTeacherSchedule(10L, null, to));
        assertStatus(400, () -> service().getTeacherSchedule(10L, from, null));
        assertStatus(400, () -> service().getTeacherSchedule(10L, to, from));

        when(teacherRepository.findById(10L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service().getTeacherSchedule(10L, from, to));
    }

    private ScheduleService service() {
        return new ScheduleService(teacherRepository, availabilityRepository, bookingRepository, courseSubjectRepository);
    }

    private static CreateAvailabilitySlotRequest request(LocalDateTime start, Long courseSubjectId) {
        return new CreateAvailabilitySlotRequest(start, courseSubjectId);
    }

    private static TeacherProfile teacher(Long profileId, Long userId, String subjectName) {
        User user = new User();
        user.setId(userId);
        user.setEmail("teacher" + userId + "@test.mn");
        user.setRole(Role.TEACHER);

        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(profileId);
        teacher.setUser(user);
        teacher.setSubjects(List.of(subjectName));
        return teacher;
    }

    private static CourseSubject subject(Long id, String name) {
        CourseSubject subject = new CourseSubject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }

    private static TeacherAvailabilitySlot slot(Long id, TeacherProfile teacher, LocalDateTime startTime, boolean booked) {
        TeacherAvailabilitySlot slot = new TeacherAvailabilitySlot();
        slot.setId(id);
        slot.setTeacherProfile(teacher);
        slot.setStartTime(startTime);
        slot.setEndTime(startTime.plusMinutes(30));
        slot.setBooked(booked);
        return slot;
    }

    private static Booking booking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setStatus(status);
        return booking;
    }

    private static void assertStatus(int status, ThrowingRunnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(status));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
