package com.tsolmon.online_teaching_platform.schedule.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * One availability slot = one lesson = exactly 30 minutes; start times align to :00 and :30.
 */
public final class ScheduleSlotRules {

    public static final int SLOT_MINUTES = 30;

    private ScheduleSlotRules() {
    }

    public static LocalDateTime endOfSlot(LocalDateTime start) {
        return start.plusMinutes(SLOT_MINUTES);
    }

    public static void validateStartBoundary(LocalDateTime start) {
        if (start.getNano() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must have zero nanoseconds");
        }
        if (start.getSecond() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time must be on a full minute (:00 seconds)");
        }
        int m = start.getMinute();
        if (m != 0 && m != 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start time must align to :00 or :30 minutes (30-minute lesson slots)"
            );
        }
    }
}
