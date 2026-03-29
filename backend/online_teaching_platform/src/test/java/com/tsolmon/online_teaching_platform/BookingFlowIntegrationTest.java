package com.tsolmon.online_teaching_platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teacherStudentBookingFlowShouldWork() throws Exception {
        String teacherToken = registerAndGetToken("Teacher One", "teacher1@test.mn", "TEACHER");
        String studentToken = registerAndGetToken("Student One", "student1@test.mn", "STUDENT");

        // Update teacher profile with searchable data.
        mockMvc.perform(put("/api/teachers/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "headline": "Java mentor",
                                  "bio": "Backend focus",
                                  "subjects": ["Java", "Spring Boot"],
                                  "skills": ["REST", "SQL"],
                                  "hourlyRate": 35
                                }
                                """))
                .andExpect(status().isOk());

        // Find teacher id from search.
        MvcResult teacherSearch = mockMvc.perform(get("/api/teachers").param("query", "java"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode teacherArray = objectMapper.readTree(teacherSearch.getResponse().getContentAsString());
        Long teacherId = teacherArray.get(0).get("id").asLong();

        // Teacher creates an available slot.
        LocalDateTime start = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        String schedulePayload = "{\"startTime\":\"" + start.format(DateTimeFormatter.ofPattern(pattern))
                + "\",\"endTime\":\"" + end.format(DateTimeFormatter.ofPattern(pattern)) + "\"}";

        MvcResult slotResult = mockMvc.perform(post("/api/schedules/me")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(schedulePayload))
                .andExpect(status().isOk())
                .andReturn();
        Long slotId = objectMapper.readTree(slotResult.getResponse().getContentAsString()).get("id").asLong();

        // Student books teacher slot.
        String bookingPayload = """
                {
                  "teacherId": %d,
                  "slotId": %d,
                  "subject": "Java",
                  "note": "Need help with Spring"
                }
                """.formatted(teacherId, slotId);

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode bookingJson = objectMapper.readTree(bookingResult.getResponse().getContentAsString());
        assertThat(bookingJson.get("status").asText()).isEqualTo("PENDING");
        Long bookingId = bookingJson.get("id").asLong();

        // Teacher confirms booking.
        MvcResult confirmResult = mockMvc.perform(patch("/api/bookings/{bookingId}/confirm", bookingId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode confirmed = objectMapper.readTree(confirmResult.getResponse().getContentAsString());
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");

        // Student can see notification entries.
        MvcResult notifications = mockMvc.perform(get("/api/notifications/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode notificationsJson = objectMapper.readTree(notifications.getResponse().getContentAsString());
        assertThat(notificationsJson.isArray()).isTrue();
        assertThat(notificationsJson.size()).isGreaterThan(0);
    }

    private String registerAndGetToken(String fullName, String email, String role) throws Exception {
        String payload = """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "password": "Password123!",
                  "role": "%s"
                }
                """.formatted(fullName, email, role);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}

