package com.tsolmon.online_teaching_platform.booking.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingLifecycleScheduler {
    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60000)
    public void runLifecycleTransitions() {
        int changed = bookingService.transitionReadyBookings(LocalDateTime.now());
        if (changed > 0) {
            log.debug("Booking lifecycle transitions applied: {}", changed);
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void runUpcomingReminderNotifications() {
        int reminders = bookingService.sendUpcomingReminderNotifications(LocalDateTime.now());
        if (reminders > 0) {
            log.debug("Booking reminder notifications sent: {}", reminders);
        }
    }
}

