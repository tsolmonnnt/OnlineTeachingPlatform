package com.tsolmon.online_teaching_platform.schedule.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * One availability slot = one lesson = exactly 30 minutes; start times align to :00 and :30.
 */
public final class ScheduleSlotRules {

    public static final int SLOT_MINUTES = 30;
    private static final int EARLIEST_START_HOUR = 6;
    /** Last allowed slot start is 21:30 (ends 22:00). */
    private static final int LATEST_START_HOUR = 21;
    private static final int LATEST_START_MINUTE = 30;

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
        validateBusinessHours(start);
    }

    private static void validateBusinessHours(LocalDateTime start) {
        int hour = start.getHour();
        int minute = start.getMinute();
        if (hour < EARLIEST_START_HOUR) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lessons are only available between 06:00 and 22:00"
            );
        }
        if (hour > LATEST_START_HOUR || (hour == LATEST_START_HOUR && minute > LATEST_START_MINUTE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lessons are only available between 06:00 and 22:00 (last start 21:30)"
            );
        }
    }
}
