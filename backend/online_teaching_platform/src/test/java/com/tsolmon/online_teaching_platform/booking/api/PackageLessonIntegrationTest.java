package com.tsolmon.online_teaching_platform.booking.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import com.tsolmon.online_teaching_platform.booking.application.BookingService;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PackageLessonIntegrationTest extends RequirementsIntegrationTestSupport {

    @Autowired
    private BookingService bookingService;

    @Test
    void packageLessonCanContinueAcrossTwoSlots() throws Exception {
        RegisteredUser teacher = registerUser("Package Teacher", "package.teacher@test.mn", "TEACHER");
        RegisteredUser student = registerUser("Package Student", "package.student@test.mn", "STUDENT");

        updateTeacherProfile(teacher.token(), "Package Java Mentor", "Java");
        long teacherProfileId = findTeacherProfileIdByQuery("Package Java Mentor");
        CourseSubject java = courseSubjectRepository.findByNameIgnoreCase("Java").orElseThrow();

        LocalDateTime firstStart = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);
        long firstSlotId = createSlot(teacher.token(), java.getId(), firstStart);
        long secondSlotId = createSlot(teacher.token(), java.getId(), firstStart.plusMinutes(30));

        String rootBody = exchange("POST", "/api/bookings", student.token(), """
                {
                  "teacherId": %d,
                  "slotId": %d,
                  "subject": "Java",
                  "bookingType": "PACKAGE_LESSON",
                  "packageTotalLessons": 2
                }
                """.formatted(teacherProfileId, firstSlotId));
        JsonNode root = objectMapper.readTree(rootBody);
        long rootId = root.get("id").asLong();
        assertThat(root.get("packageTotalLessons").asInt()).isEqualTo(2);
        assertThat(root.get("packageCompletedLessons").asInt()).isZero();

        exchange("PATCH", "/api/bookings/" + rootId + "/confirm", teacher.token(), null);

        String childBody = exchange("POST", "/api/bookings", student.token(), """
                {
                  "teacherId": %d,
                  "slotId": %d,
                  "parentBookingId": %d,
                  "subject": "Java"
                }
                """.formatted(teacherProfileId, secondSlotId, rootId));
        JsonNode child = objectMapper.readTree(childBody);
        long childId = child.get("id").asLong();
        assertThat(child.get("parentBookingId").asLong()).isEqualTo(rootId);
        // Both lessons are now booked, consuming both credits.
        assertThat(child.get("packageBookedLessons").asInt()).isEqualTo(2);

        exchange("PATCH", "/api/bookings/" + childId + "/confirm", teacher.token(), null);

        LocalDateTime afterBoth = firstStart.plusMinutes(60);
        // Run the scheduler twice: completion must be exactly-once (no double counting on re-runs).
        bookingService.transitionReadyBookings(afterBoth);
        bookingService.transitionReadyBookings(afterBoth.plusMinutes(5));

        JsonNode studentBookings = objectMapper.readTree(exchange("GET", "/api/bookings/me", student.token(), null));
        JsonNode rootAfter = null;
        for (JsonNode row : studentBookings) {
            if (row.get("id").asLong() == rootId) {
                rootAfter = row;
                break;
            }
        }
        assertThat(rootAfter).isNotNull();
        assertThat(rootAfter.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(rootAfter.get("packageCompletedLessons").asInt()).isEqualTo(2);
    }
}
