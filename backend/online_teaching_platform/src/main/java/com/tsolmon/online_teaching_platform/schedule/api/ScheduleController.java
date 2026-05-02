package com.tsolmon.online_teaching_platform.schedule.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.schedule.api.dto.AvailabilitySlotResponse;
import com.tsolmon.online_teaching_platform.schedule.api.dto.CreateAvailabilitySlotRequest;
import com.tsolmon.online_teaching_platform.schedule.application.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping("/me")
    public List<AvailabilitySlotResponse> mySchedule(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return scheduleService.getMySchedule(authUser);
    }

    @PostMapping("/me")
    public AvailabilitySlotResponse addMySlot(
            Authentication authentication,
            @Valid @RequestBody CreateAvailabilitySlotRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return scheduleService.addMySlot(authUser, request);
    }

    @PutMapping("/me/{slotId}")
    public AvailabilitySlotResponse updateMySlot(
            Authentication authentication,
            @PathVariable Long slotId,
            @Valid @RequestBody CreateAvailabilitySlotRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return scheduleService.updateMySlot(authUser, slotId, request);
    }

    @DeleteMapping("/me/{slotId}")
    public void deleteMySlot(Authentication authentication, @PathVariable Long slotId) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        scheduleService.deleteMySlot(authUser, slotId);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<AvailabilitySlotResponse> teacherSchedule(
            @PathVariable Long teacherId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        return scheduleService.getTeacherSchedule(teacherId, from, to);
    }
}

