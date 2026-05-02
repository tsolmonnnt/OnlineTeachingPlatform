package com.tsolmon.online_teaching_platform.schedule.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleSlotRulesTest {

    @Test
    void endOfSlotAdds30Minutes() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 3, 10, 0);
        assertThat(ScheduleSlotRules.endOfSlot(start)).isEqualTo(LocalDateTime.of(2026, 5, 3, 10, 30));
    }

    @Test
    void acceptsHourAndHalfHourStarts() {
        ScheduleSlotRules.validateStartBoundary(LocalDateTime.of(2026, 5, 3, 10, 0));
        ScheduleSlotRules.validateStartBoundary(LocalDateTime.of(2026, 5, 3, 10, 30));
    }

    @Test
    void rejectsWrongMinute() {
        assertThatThrownBy(() -> ScheduleSlotRules.validateStartBoundary(LocalDateTime.of(2026, 5, 3, 10, 17)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void rejectsNonZeroSeconds() {
        LocalDateTime bad = LocalDateTime.of(2026, 5, 3, 10, 0, 1);
        assertThatThrownBy(() -> ScheduleSlotRules.validateStartBoundary(bad))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value()));
    }
}
