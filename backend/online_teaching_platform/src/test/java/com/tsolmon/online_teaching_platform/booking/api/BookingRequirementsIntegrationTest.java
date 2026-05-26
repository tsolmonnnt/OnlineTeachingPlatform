package com.tsolmon.online_teaching_platform.booking.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void bookingScheduleAndNotificationPathsShouldWork() throws Exception {
        RegisteredUser teacher = registerUser("Coverage Teacher", "coverage.teacher@test.mn", "TEACHER");
        RegisteredUser otherTeacher = registerUser("Coverage Other Teacher", "coverage.other.teacher@test.mn", "TEACHER");
        RegisteredUser student = registerUser("Coverage Student", "coverage.student@test.mn", "STUDENT");
        RegisteredUser admin = registerUser("Coverage Admin", "coverage.admin@test.mn", "ADMIN");

        updateTeacherProfile(teacher.token(), "Coverage Java Mentor", "Java");
        updateTeacherProfile(otherTeacher.token(), "Coverage Other Mentor", "Java");
        long teacherProfileId = findTeacherProfileIdByQuery("Coverage Java Mentor");
        long otherTeacherProfileId = findTeacherProfileIdByQuery("Coverage Other Mentor");
        CourseSubject java = courseSubjectRepository.findByNameIgnoreCase("Java").orElseThrow();

        LocalDateTime firstStart = LocalDateTime.now().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);
        long firstSlotId = createSlot(teacher.token(), java.getId(), firstStart);
        long secondSlotId = createSlot(teacher.token(), java.getId(), firstStart.plusMinutes(30));

        JsonNode mySchedule = objectMapper.readTree(exchange("GET", "/api/schedules/me", teacher.token(), null));
        assertThat(mySchedule.size()).isGreaterThanOrEqualTo(2);

        String from = firstStart.minusHours(1).format(ISO_SECONDS);
        String to = firstStart.plusHours(2).format(ISO_SECONDS);
        JsonNode publicSchedule = objectMapper.readTree(exchange(
                "GET",
                "/api/schedules/teacher/" + teacherProfileId + "?from=" + from + "&to=" + to,
                null,
                null
        ));
        assertThat(publicSchedule.size()).isGreaterThanOrEqualTo(2);
        assertThat(exchangeStatus(
                "GET",
                "/api/schedules/teacher/" + teacherProfileId + "?from=" + to + "&to=" + from,
                null,
                null
        )).isEqualTo(400);

        LocalDateTime updatedStart = firstStart.plusDays(1);
        JsonNode updatedSlot = objectMapper.readTree(exchange("PUT", "/api/schedules/me/" + firstSlotId, teacher.token(), """
                {
                  "startTime": "%s",
                  "courseSubjectId": %d
                }
                """.formatted(updatedStart.format(ISO_SECONDS), java.getId())));
        assertThat(updatedSlot.get("startTime").asText()).startsWith(updatedStart.toLocalDate().toString());

        assertThat(exchangeStatus("DELETE", "/api/schedules/me/" + firstSlotId, otherTeacher.token(), null)).isEqualTo(403);
        assertThat(exchangeStatus("DELETE", "/api/schedules/me/" + firstSlotId, teacher.token(), null)).isBetween(200, 299);

        String bookingBody = exchange("POST", "/api/bookings", student.token(), """
                {
                  "teacherId": %d,
                  "slotId": %d,
                  "subject": "Java",
                  "note": "Coverage booking"
                }
                """.formatted(teacherProfileId, secondSlotId));
        long bookingId = objectMapper.readTree(bookingBody).get("id").asLong();

        assertThat(exchangeStatus("PATCH", "/api/bookings/" + bookingId + "/confirm", otherTeacher.token(), null)).isEqualTo(403);
        assertThat(objectMapper.readTree(exchange("PATCH", "/api/bookings/" + bookingId + "/cancel", student.token(), null))
                .get("status").asText()).isEqualTo("CANCELLED");
        assertThat(exchangeStatus("PATCH", "/api/bookings/" + bookingId + "/confirm", teacher.token(), null)).isEqualTo(409);

        assertThat(objectMapper.readTree(exchange("GET", "/api/bookings/me", student.token(), null)).isArray()).isTrue();
        assertThat(objectMapper.readTree(exchange("GET", "/api/bookings/me", teacher.token(), null)).isArray()).isTrue();
        assertThat(objectMapper.readTree(exchange("GET", "/api/bookings/me", admin.token(), null)).isArray()).isTrue();

        assertThat(exchangeStatus("DELETE", "/api/schedules/me/" + secondSlotId, teacher.token(), null)).isBetween(200, 299);

        JsonNode notifications = objectMapper.readTree(exchange("GET", "/api/notifications/me", student.token(), null));
        assertThat(notifications.size()).isGreaterThan(0);
        long notificationId = notifications.get(0).get("id").asLong();
        JsonNode read = objectMapper.readTree(exchange("PATCH", "/api/notifications/" + notificationId + "/read", student.token(), null));
        assertThat(read.get("isRead").asBoolean()).isTrue();
        assertThat(exchangeStatus("PATCH", "/api/notifications/" + notificationId + "/read", teacher.token(), null)).isEqualTo(403);
        assertThat(otherTeacherProfileId).isGreaterThan(0);
    }
}
