package com.tsolmon.online_teaching_platform.booking.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.api.dto.BookingResponse;
import com.tsolmon.online_teaching_platform.booking.api.dto.CreateBookingRequest;
import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.notification.application.NotificationService;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilityRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.entity.User;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public BookingResponse createBooking(AuthUser authUser, CreateBookingRequest request) {
        if (authUser.role() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can create bookings");
        }

        TeacherProfile teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        TeacherAvailabilitySlot slot = availabilityRepository.findById(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));

        if (!slot.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected slot does not belong to this teacher");
        }
        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected slot is already booked");
        }
        if (slot.getCourseSubject() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This availability slot has no course attached; the teacher must recreate it"
            );
        }

        User student = userRepository.findById(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        String subjectLine;
        if (request.subject() != null && !request.subject().isBlank()) {
            subjectLine = request.subject().trim();
            if (!subjectLine.equalsIgnoreCase(slot.getCourseSubject().getName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Subject must match the slot course: " + slot.getCourseSubject().getName()
                );
            }
        } else {
            subjectLine = slot.getCourseSubject().getName();
        }

        slot.setBooked(true);
        availabilityRepository.save(slot);

        Booking booking = new Booking();
        booking.setStudentUser(student);
        booking.setTeacherProfile(teacher);
        booking.setAvailabilitySlot(slot);
        booking.setCourseSubject(slot.getCourseSubject());
        booking.setSubject(subjectLine);
        booking.setNote(request.note());
        booking.setStatus(BookingStatus.PENDING);

        Booking saved = bookingRepository.save(booking);

        notificationService.notifyUser(
                teacher.getUser().getId(),
                "New booking request",
                student.getFullName() + " booked a lesson for " + subjectLine
        );
        notificationService.notifyUser(
                student.getId(),
                "Booking created",
                "Your booking request is pending teacher confirmation"
        );

        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(AuthUser authUser) {
        if (authUser.role() == Role.TEACHER) {
            TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
            return bookingRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(teacher.getId())
                    .stream().map(BookingResponse::from).toList();
        }
        if (authUser.role() == Role.ADMIN) {
            return bookingRepository.findAll().stream().map(BookingResponse::from).toList();
        }
        return bookingRepository.findByStudentUser_IdOrderByCreatedAtDesc(authUser.id())
                .stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse confirmBooking(AuthUser authUser, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        ensureTeacherOwnsBooking(authUser, booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled booking cannot be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        notificationService.notifyUser(
                booking.getStudentUser().getId(),
                "Booking confirmed",
                booking.getTeacherProfile().getUser().getFullName() + " confirmed your booking"
        );

        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(AuthUser authUser, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        boolean canCancel = authUser.role() == Role.ADMIN
                || booking.getStudentUser().getId().equals(authUser.id())
                || (authUser.role() == Role.TEACHER
                && booking.getTeacherProfile().getUser().getId().equals(authUser.id()));

        if (!canCancel) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        TeacherAvailabilitySlot slot = booking.getAvailabilitySlot();
        slot.setBooked(false);
        availabilityRepository.save(slot);

        Booking saved = bookingRepository.save(booking);

        notificationService.notifyUser(
                booking.getStudentUser().getId(),
                "Booking cancelled",
                "Booking for " + booking.getSubject() + " has been cancelled"
        );
        notificationService.notifyUser(
                booking.getTeacherProfile().getUser().getId(),
                "Booking cancelled",
                "Booking for " + booking.getSubject() + " has been cancelled"
        );

        return BookingResponse.from(saved);
    }

    private void ensureTeacherOwnsBooking(AuthUser authUser, Booking booking) {
        if (authUser.role() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can confirm bookings");
        }
        if (!booking.getTeacherProfile().getUser().getId().equals(authUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot confirm another teacher's booking");
        }
    }
}
