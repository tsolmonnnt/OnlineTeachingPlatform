package com.tsolmon.online_teaching_platform.review.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import com.tsolmon.online_teaching_platform.booking.application.BookingService;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {
    @Autowired
    private BookingService bookingService;

    @Test
    void fr09ReviewsShouldRequireConfirmedOwnStudentBookingAndPreventDuplicates() throws Exception {
        RegisteredUser teacher = registerUser("Teacher review", "review.teacher@test.mn", "TEACHER");
        RegisteredUser student = registerUser("Student review", "review.student@test.mn", "STUDENT");
        updateTeacherProfile(teacher.token(), "Java mentor review", "Java");

        long teacherProfileId = findTeacherProfileIdByQuery("Java mentor review");
        CourseSubject java = courseSubjectRepository.findByNameIgnoreCase("Java").orElseThrow();
        LocalDateTime start = LocalDateTime.now().minusHours(3).withMinute(0).withSecond(0).withNano(0);
        long slotId = createSlot(teacher.token(), java.getId(), start);

        String bookingBody = exchange("POST", "/api/bookings", student.token(), """
                {
                  "teacherId": %d,
                  "slotId": %d,
                  "subject": "Java",
                  "note": "Need Java help"
                }
                """.formatted(teacherProfileId, slotId));
        long bookingId = objectMapper.readTree(bookingBody).get("id").asLong();
        exchange("PATCH", "/api/bookings/" + bookingId + "/confirm", teacher.token(), null);
        bookingService.transitionReadyBookings(LocalDateTime.now().plusDays(30));

        String reviewBody = exchange("POST", "/api/reviews", student.token(), """
                {
                  "bookingId": %d,
                  "rating": 5,
                  "comment": "Clear explanations"
                }
                """.formatted(bookingId));
        JsonNode review = objectMapper.readTree(reviewBody);
        assertThat(review.get("bookingId").asLong()).isEqualTo(bookingId);
        assertThat(review.get("rating").asInt()).isEqualTo(5);

        assertThat(exchangeStatus("POST", "/api/reviews", student.token(), """
                {
                  "bookingId": %d,
                  "rating": 4,
                  "comment": "Second review"
                }
                """.formatted(bookingId))).isEqualTo(409);

        assertThat(exchangeStatus("POST", "/api/reviews", teacher.token(), """
                {
                  "bookingId": %d,
                  "rating": 5,
                  "comment": "Teacher cannot review"
                }
                """.formatted(bookingId))).isEqualTo(403);

        String listBody = exchange("GET", "/api/reviews/teacher/" + teacherProfileId, null, null);
        JsonNode reviews = objectMapper.readTree(listBody);
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).get("comment").asText()).isEqualTo("Clear explanations");
    }
}
