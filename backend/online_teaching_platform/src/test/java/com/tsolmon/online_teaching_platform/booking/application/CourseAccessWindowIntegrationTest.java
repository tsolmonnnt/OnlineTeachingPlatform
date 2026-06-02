package com.tsolmon.online_teaching_platform.booking.application;

import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CourseAccessWindowIntegrationTest extends RequirementsIntegrationTestSupport {

    @Autowired
    private CourseAccessService courseAccessService;

    @Test
    void accessDependsOnStartedLessonWithinWindow() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("aw");
        LocalDateTime now = LocalDateTime.now();

        // A lesson that has not started yet must not grant access (no far-future loophole).
        setBookingSlotTimes(access.slotId(), now.plusDays(10), now.plusDays(10).plusHours(1));
        assertThat(courseAccessService.hasCourseAccess(
                access.studentUserId(), access.teacherProfileId(), access.courseSubjectId(), now)).isFalse();

        // A lesson that started and ended within the trailing window grants access.
        setBookingSlotTimes(access.slotId(), now.minusHours(2), now.minusHours(1));
        assertThat(courseAccessService.hasCourseAccess(
                access.studentUserId(), access.teacherProfileId(), access.courseSubjectId(), now)).isTrue();

        // Once the lesson is older than the window, access expires.
        setBookingSlotTimes(access.slotId(), now.minusDays(40).minusHours(1), now.minusDays(40));
        assertThat(courseAccessService.hasCourseAccess(
                access.studentUserId(), access.teacherProfileId(), access.courseSubjectId(), now)).isFalse();
    }
}
