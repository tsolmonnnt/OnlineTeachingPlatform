package com.tsolmon.online_teaching_platform.schedule.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.booking.domain.Booking;
import com.tsolmon.online_teaching_platform.booking.domain.BookingRepository;
import com.tsolmon.online_teaching_platform.booking.domain.BookingStatus;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.schedule.api.dto.AvailabilitySlotResponse;
import com.tsolmon.online_teaching_platform.schedule.api.dto.CreateAvailabilitySlotRequest;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilityRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final TeacherRepository teacherRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final CourseSubjectRepository courseSubjectRepository;

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getMySchedule(AuthUser authUser) {
        TeacherProfile teacher = findTeacherByUserId(authUser.id());
        return availabilityRepository.findByTeacherProfile_IdOrderByStartTimeAsc(teacher.getId()).stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
    }

    @Transactional
    public AvailabilitySlotResponse addMySlot(AuthUser authUser, CreateAvailabilitySlotRequest request) {
        TeacherProfile teacher = findTeacherByUserId(authUser.id());
        ScheduleSlotRules.validateStartBoundary(request.startTime());
        LocalDateTime endTime = ScheduleSlotRules.endOfSlot(request.startTime());

        CourseSubject courseSubject = courseSubjectRepository.findById(request.courseSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown course subject"));
        if (!teacherOffersSubject(teacher, courseSubject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add this subject to your teacher profile before offering slots for it");
        }

        if (!endTime.isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slot duration");
        }

        boolean overlaps = availabilityRepository.existsByTeacherProfile_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                teacher.getId(),
                endTime,
                request.startTime()
        );
        if (overlaps) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This 30-minute slot overlaps an existing slot (same teacher cannot teach two lessons at once)"
            );
        }

        TeacherAvailabilitySlot slot = new TeacherAvailabilitySlot();
        slot.setTeacherProfile(teacher);
        slot.setStartTime(request.startTime());
        slot.setEndTime(endTime);
        slot.setCourseSubject(courseSubject);
        slot.setBooked(false);
        return AvailabilitySlotResponse.from(availabilityRepository.save(slot));
    }

    /**
     * Changes start time and/or course for an unbooked slot; same 30-minute and overlap rules as {@link #addMySlot}.
     */
    @Transactional
    public AvailabilitySlotResponse updateMySlot(AuthUser authUser, Long slotId, CreateAvailabilitySlotRequest request) {
        TeacherProfile teacher = findTeacherByUserId(authUser.id());
        TeacherAvailabilitySlot slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));

        if (!slot.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another teacher's slot");
        }
        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change an already booked slot");
        }

        ScheduleSlotRules.validateStartBoundary(request.startTime());
        LocalDateTime endTime = ScheduleSlotRules.endOfSlot(request.startTime());
        if (!endTime.isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slot duration");
        }

        CourseSubject courseSubject = courseSubjectRepository.findById(request.courseSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown course subject"));
        if (!teacherOffersSubject(teacher, courseSubject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add this subject to your teacher profile before offering slots for it");
        }

        boolean overlaps = availabilityRepository.existsByTeacherProfile_IdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                teacher.getId(),
                slotId,
                endTime,
                request.startTime()
        );
        if (overlaps) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This 30-minute slot overlaps an existing slot (same teacher cannot teach two lessons at once)"
            );
        }

        slot.setStartTime(request.startTime());
        slot.setEndTime(endTime);
        slot.setCourseSubject(courseSubject);
        return AvailabilitySlotResponse.from(availabilityRepository.save(slot));
    }

    private static boolean teacherOffersSubject(TeacherProfile teacher, CourseSubject subject) {
        return teacher.getSubjects().stream().anyMatch(s -> s.equalsIgnoreCase(subject.getName()));
    }

    @Transactional
    public void deleteMySlot(AuthUser authUser, Long slotId) {
        TeacherProfile teacher = findTeacherByUserId(authUser.id());
        TeacherAvailabilitySlot slot = availabilityRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));

        if (!slot.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another teacher's slot");
        }
        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete an already booked slot");
        }

        List<Booking> linkedBookings = bookingRepository.findByAvailabilitySlot_Id(slotId);
        for (Booking booking : linkedBookings) {
            if (booking.getStatus() != BookingStatus.CANCELLED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete slot: it still has a booking that is not cancelled"
                );
            }
        }
        bookingRepository.deleteAll(linkedBookings);

        availabilityRepository.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getTeacherSchedule(Long teacherId, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide valid from/to date range");
        }

        TeacherProfile teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        return availabilityRepository
                .findOverlappingTeacherSchedule(teacher.getId(), from, to)
                .stream()
                .map(AvailabilitySlotResponse::from)
                .toList();
    }

    private TeacherProfile findTeacherByUserId(Long userId) {
        return teacherRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }
}

