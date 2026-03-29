package com.tsolmon.online_teaching_platform.schedule.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
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
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        boolean overlaps = availabilityRepository.existsByTeacherProfile_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                teacher.getId(),
                request.endTime(),
                request.startTime()
        );
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Time slot overlaps with an existing slot");
        }

        TeacherAvailabilitySlot slot = new TeacherAvailabilitySlot();
        slot.setTeacherProfile(teacher);
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setBooked(false);
        return AvailabilitySlotResponse.from(availabilityRepository.save(slot));
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
                .findByTeacherProfile_IdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
                        teacher.getId(),
                        from,
                        to
                ).stream().map(AvailabilitySlotResponse::from).toList();
    }

    private TeacherProfile findTeacherByUserId(Long userId) {
        return teacherRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
    }
}

