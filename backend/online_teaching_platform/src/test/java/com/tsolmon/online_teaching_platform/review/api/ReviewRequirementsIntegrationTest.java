package com.tsolmon.online_teaching_platform.review.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void fr09ReviewsShouldRequireConfirmedOwnStudentBookingAndPreventDuplicates() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("review");

        String reviewBody = exchange("POST", "/api/reviews", access.studentToken(), """
                {
                  "bookingId": %d,
                  "rating": 5,
                  "comment": "Clear explanations"
                }
                """.formatted(access.bookingId()));
        JsonNode review = objectMapper.readTree(reviewBody);
        assertThat(review.get("bookingId").asLong()).isEqualTo(access.bookingId());
        assertThat(review.get("rating").asInt()).isEqualTo(5);

        assertThat(exchangeStatus("POST", "/api/reviews", access.studentToken(), """
                {
                  "bookingId": %d,
                  "rating": 4,
                  "comment": "Second review"
                }
                """.formatted(access.bookingId()))).isEqualTo(409);

        assertThat(exchangeStatus("POST", "/api/reviews", access.teacherToken(), """
                {
                  "bookingId": %d,
                  "rating": 5,
                  "comment": "Teacher cannot review"
                }
                """.formatted(access.bookingId()))).isEqualTo(403);

        String listBody = exchange("GET", "/api/reviews/teacher/" + access.teacherProfileId(), null, null);
        JsonNode reviews = objectMapper.readTree(listBody);
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).get("comment").asText()).isEqualTo("Clear explanations");
    }
}
