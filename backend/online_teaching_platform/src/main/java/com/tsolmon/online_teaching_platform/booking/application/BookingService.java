package com.tsolmon.online_teaching_platform.booking.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.api.dto.BookingResponse;
import com.tsolmon.online_teaching_platform.booking.api.dto.CreateBookingRequest;
import com.tsolmon.online_teaching_platform.booking.api.dto.UpdateMeetingLinkRequest;
import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.booking.domain.BookingType;
import com.tsolmon.online_teaching_platform.notification.application.NotificationService;
import com.tsolmon.online_teaching_platform.review.domain.ReviewRepository;
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

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;
    private static final long REVIEW_WINDOW_HOURS = 48;

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
        booking.setMeetingLink(null);
        booking.setReminderSent(false);

        if (request.parentBookingId() != null) {
            attachPackageContinuation(booking, student, teacher, request.parentBookingId());
        } else {
            BookingType bookingType = request.bookingType() == null ? BookingType.SINGLE_LESSON : request.bookingType();
            booking.setBookingType(bookingType);
            booking.setParentBooking(null);
            applyBookingTypeFields(booking, bookingType, request);
        }

        Booking saved = bookingRepository.save(booking);

        notificationService.notifyUser(
                teacher.getUser().getId(),
                "Шинэ захиалгын хүсэлт",
                student.getFullName() + " «" + subjectLine + "» хичээлд захиалга илгээлээ"
        );
        notificationService.notifyUser(
                student.getId(),
                "Захиалга үүслээ",
                "Таны захиалга багшийн баталгаажуулалтыг хүлээж байна"
        );

        return BookingResponse.from(saved, false, null, packageBookedLessonsFor(saved));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(AuthUser authUser) {
        if (authUser.role() == Role.TEACHER) {
            TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
            return bookingRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(teacher.getId())
                    .stream()
                    .map(booking -> BookingResponse.from(booking, false, null, packageBookedLessonsFor(booking)))
                    .toList();
        }
        if (authUser.role() == Role.ADMIN) {
            return bookingRepository.findAll().stream()
                    .map(booking -> BookingResponse.from(booking, false, null, packageBookedLessonsFor(booking)))
                    .toList();
        }
        return bookingRepository.findByStudentUser_IdOrderByCreatedAtDesc(authUser.id())
                .stream()
                .map(booking -> {
                    LocalDateTime reviewDeadline = resolveReviewDeadline(booking);
                    boolean canReview = canStudentReview(booking, authUser.id(), reviewDeadline);
                    return BookingResponse.from(booking, canReview, reviewDeadline, packageBookedLessonsFor(booking));
                })
                .toList();
    }

    @Transactional
    public BookingResponse confirmBooking(AuthUser authUser, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        ensureTeacherOwnsBooking(authUser, booking);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        notificationService.notifyUser(
                booking.getStudentUser().getId(),
                "Захиалга батлагдлаа",
                booking.getTeacherProfile().getUser().getFullName() + " таны захиалгыг баталгаажууллаа"
        );

        return BookingResponse.from(saved, false, null, packageBookedLessonsFor(saved));
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

        if (booking.getStatus() == BookingStatus.REVIEWED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reviewed booking cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        TeacherAvailabilitySlot slot = booking.getAvailabilitySlot();
        slot.setBooked(false);
        availabilityRepository.save(slot);

        Booking saved = bookingRepository.save(booking);

        String cancelledMessage = "«" + booking.getSubject() + "» хичээлийн захиалга цуцлагдлаа";
        notificationService.notifyUser(
                booking.getStudentUser().getId(),
                "Захиалга цуцлагдлаа",
                cancelledMessage
        );
        notificationService.notifyUser(
                booking.getTeacherProfile().getUser().getId(),
                "Захиалга цуцлагдлаа",
                cancelledMessage
        );

        return BookingResponse.from(saved, false, null, packageBookedLessonsFor(saved));
    }

    @Transactional
    public BookingResponse updateMeetingLink(AuthUser authUser, Long bookingId, UpdateMeetingLinkRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        ensureTeacherOwnsBooking(authUser, booking);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled booking cannot have a meeting link");
        }
        booking.setMeetingLink(request.meetingLink().trim());
        Booking saved = bookingRepository.save(booking);
        return BookingResponse.from(saved, false, null, packageBookedLessonsFor(saved));
    }

    @Transactional
    public int transitionReadyBookings(LocalDateTime now) {
        int changed = 0;
        List<Booking> readyToStart = bookingRepository.findReadyToStart(BookingStatus.CONFIRMED, now);
        for (Booking booking : readyToStart) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.IN_PROGRESS);
                changed++;
            }
        }

        List<Booking> readyToComplete = bookingRepository.findReadyToComplete(
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS),
                now
        );
        for (Booking booking : readyToComplete) {
            if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REVIEWED) {
                continue;
            }
            LocalDateTime slotEnd = booking.getAvailabilitySlot().getEndTime();
            if (slotEnd == null || slotEnd.isAfter(now)) {
                continue;
            }

            if (booking.getBookingType() == BookingType.PACKAGE_LESSON) {
                if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
                    continue;
                }
                completePackageLesson(booking);
                changed++;
                continue;
            }

            if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
                booking.setStatus(BookingStatus.COMPLETED);
                changed++;
            }
        }
        return changed;
    }

    @Transactional
    public int sendUpcomingReminderNotifications(LocalDateTime now) {
        LocalDateTime limit = now.plusMinutes(15);
        List<Booking> candidates = bookingRepository.findReminderCandidates(BookingStatus.CONFIRMED, now, limit);
        int sent = 0;
        for (Booking booking : candidates) {
            if (booking.isReminderSent() || booking.getStatus() != BookingStatus.CONFIRMED) {
                continue;
            }
            notificationService.notifyUser(
                    booking.getStudentUser().getId(),
                    "Өнөөдрийн хичээлд бэлэн үү?",
                    "Таны хичээл 15 минутын дараа эхэлнэ."
            );
            booking.setReminderSent(true);
            sent++;
        }
        return sent;
    }

    private void ensureTeacherOwnsBooking(AuthUser authUser, Booking booking) {
        if (authUser.role() != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers can confirm bookings");
        }
        if (!booking.getTeacherProfile().getUser().getId().equals(authUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot confirm another teacher's booking");
        }
    }

    private void applyBookingTypeFields(Booking booking, BookingType bookingType, CreateBookingRequest request) {
        if (bookingType == BookingType.PACKAGE_LESSON) {
            if (request.packageTotalLessons() == null || request.packageTotalLessons() < 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "packageTotalLessons must be at least 2 for PACKAGE_LESSON"
                );
            }
            booking.setPackageTotalLessons(request.packageTotalLessons());
            booking.setPackageCompletedLessons(0);
            return;
        }
        booking.setPackageTotalLessons(null);
        booking.setPackageCompletedLessons(null);
    }

    private void attachPackageContinuation(Booking booking, User student, TeacherProfile teacher, Long parentBookingId) {
        Booking parent = bookingRepository.findById(parentBookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package booking not found"));
        if (parent.getParentBooking() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only continue from the root package booking");
        }
        if (parent.getBookingType() != BookingType.PACKAGE_LESSON) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent booking is not a package lesson");
        }
        if (!parent.getStudentUser().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot continue another student's package");
        }
        if (!parent.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package continuation must use the same teacher");
        }
        if (parent.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package booking is no longer active");
        }
        int total = parent.getPackageTotalLessons() == null ? 0 : parent.getPackageTotalLessons();
        if (bookedPackageLessons(parent) >= total) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package fully scheduled");
        }
        booking.setBookingType(BookingType.PACKAGE_LESSON);
        booking.setParentBooking(parent);
        booking.setPackageTotalLessons(null);
        booking.setPackageCompletedLessons(null);
    }

    /**
     * Number of credits a package has consumed: the root lesson (unless cancelled) plus each non-cancelled child.
     * Cancelling a lesson frees its credit so the student can rebook.
     */
    private int bookedPackageLessons(Booking root) {
        long children = bookingRepository.countByParentBooking_IdAndStatusNot(root.getId(), BookingStatus.CANCELLED);
        int rootLesson = root.getStatus() == BookingStatus.CANCELLED ? 0 : 1;
        return (int) children + rootLesson;
    }

    /** Booked-credit count for the package this booking belongs to, or null for non-package bookings. */
    private Integer packageBookedLessonsFor(Booking booking) {
        if (booking.getBookingType() != BookingType.PACKAGE_LESSON) {
            return null;
        }
        return bookedPackageLessons(resolvePackageRoot(booking));
    }

    /**
     * Completes one package lesson exactly once. Each lesson (root or child) transitions IN_PROGRESS -> COMPLETED
     * terminally, so the scheduler never re-processes it. The root's progress counter is the package progress;
     * the package's "still has credits" state is derived from booked lessons, not the root status.
     */
    private void completePackageLesson(Booking lessonBooking) {
        Booking packageRoot = resolvePackageRoot(lessonBooking);
        int completed = packageRoot.getPackageCompletedLessons() == null ? 0 : packageRoot.getPackageCompletedLessons();
        completed++;
        packageRoot.setPackageCompletedLessons(completed);
        lessonBooking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(lessonBooking);
        if (!packageRoot.getId().equals(lessonBooking.getId())) {
            bookingRepository.save(packageRoot);
        }
    }

    private Booking resolvePackageRoot(Booking booking) {
        if (booking.getParentBooking() != null) {
            return booking.getParentBooking();
        }
        return booking;
    }

    private LocalDateTime resolveReviewDeadline(Booking booking) {
        LocalDateTime end = booking.getAvailabilitySlot().getEndTime();
        if (end == null) {
            return null;
        }
        return end.plusHours(REVIEW_WINDOW_HOURS);
    }

    private boolean canStudentReview(Booking booking, Long studentUserId, LocalDateTime reviewDeadline) {
        if (!booking.getStudentUser().getId().equals(studentUserId)) {
            return false;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REVIEWED) {
            return false;
        }
        LocalDateTime end = booking.getAvailabilitySlot().getEndTime();
        if (end == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(end)) {
            return false;
        }
        if (reviewDeadline == null || now.isAfter(reviewDeadline)) {
            return false;
        }
        if (!EnumSet.of(BookingStatus.COMPLETED, BookingStatus.IN_PROGRESS, BookingStatus.CONFIRMED).contains(booking.getStatus())) {
            return false;
        }
        return !reviewRepository.existsByBooking_Id(booking.getId());
    }
}
