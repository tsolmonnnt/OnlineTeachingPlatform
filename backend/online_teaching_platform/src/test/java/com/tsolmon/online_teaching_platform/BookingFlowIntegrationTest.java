package com.tsolmon.online_teaching_platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void teachingSubjectsEndpointReturnsOnlyProfileMatchesForTeacher() throws Exception {
        String teacherToken = registerAndGetToken("Teacher Courses", "teachercourses@test.mn", "TEACHER");
        String studentToken = registerAndGetToken("Student Courses", "studentcourses@test.mn", "STUDENT");

        exchange("PUT", "/api/teachers/me", teacherToken, """
                {
                  "headline": "H",
                  "bio": "B",
                  "subjects": ["Java", "NonexistentSubjectXYZ"],
                  "skills": ["REST"],
                  "hourlyRate": 35
                }
                """);

        String body = exchange("GET", "/api/course/subjects/teaching", teacherToken, null);
        JsonNode arr = objectMapper.readTree(body);
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("name").asText()).isEqualTo("Java");

        assertThat(exchangeStatus("GET", "/api/course/subjects/teaching", studentToken, null)).isEqualTo(403);
    }

    @Test
    void teacherStudentBookingFlowShouldWork() throws Exception {
        String teacherToken = registerAndGetToken("Teacher One", "teacher1@test.mn", "TEACHER");
        String studentToken = registerAndGetToken("Student One", "student1@test.mn", "STUDENT");

        exchange("PUT", "/api/teachers/me", teacherToken, """
                {
                  "headline": "Java mentor",
                  "bio": "Backend focus",
                  "subjects": ["Java", "Spring Boot"],
                  "skills": ["REST", "SQL"],
                  "hourlyRate": 35
                }
                """);

        String teacherSearchBody = exchange("GET", "/api/teachers?query=java", null, null);
        JsonNode teacherArray = objectMapper.readTree(teacherSearchBody);
        Long teacherId = teacherArray.get(0).get("id").asLong();

        String subjectsBody = exchange("GET", "/api/course/subjects", null, null);
        JsonNode subjectsArr = objectMapper.readTree(subjectsBody);
        long courseSubjectId = -1;
        for (JsonNode s : subjectsArr) {
            if ("Java".equals(s.get("name").asText())) {
                courseSubjectId = s.get("id").asLong();
                break;
            }
        }
        assertThat(courseSubjectId).isGreaterThan(0);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        String schedulePayload = "{\"startTime\":\"" + start.format(DateTimeFormatter.ofPattern(pattern))
                + "\",\"courseSubjectId\":" + courseSubjectId + "}";

        String slotJson = exchange("POST", "/api/schedules/me", teacherToken, schedulePayload);
        Long slotId = objectMapper.readTree(slotJson).get("id").asLong();

        String bookingPayload = """
                {
                  "teacherId": %d,
                  "slotId": %d,
                  "subject": "Java",
                  "note": "Need help with Spring"
                }
                """.formatted(teacherId, slotId); // subject matches slot course (Java)

        String bookingJsonRaw = exchange("POST", "/api/bookings", studentToken, bookingPayload);
        JsonNode bookingJson = objectMapper.readTree(bookingJsonRaw);
        assertThat(bookingJson.get("status").asText()).isEqualTo("PENDING");
        Long bookingId = bookingJson.get("id").asLong();

        String confirmRaw = exchange("PATCH", "/api/bookings/" + bookingId + "/confirm", teacherToken, null);
        JsonNode confirmed = objectMapper.readTree(confirmRaw);
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");

        String notificationsBody = exchange("GET", "/api/notifications/me", studentToken, null);
        JsonNode notificationsJson = objectMapper.readTree(notificationsBody);
        assertThat(notificationsJson.isArray()).isTrue();
        assertThat(notificationsJson.size()).isGreaterThan(0);
    }

    @Test
    void scheduleInvalidMinuteReturns400() throws Exception {
        String teacherToken = registerAndGetToken("Teacher BadSlot", "teacherbadslot@test.mn", "TEACHER");
        exchange("PUT", "/api/teachers/me", teacherToken, """
                {
                  "headline": "T",
                  "bio": "B",
                  "subjects": ["Java"],
                  "skills": ["REST"],
                  "hourlyRate": 35
                }
                """);
        String subjectsBody = exchange("GET", "/api/course/subjects", null, null);
        JsonNode subjectsArr = objectMapper.readTree(subjectsBody);
        long courseSubjectId = -1;
        for (JsonNode s : subjectsArr) {
            if ("Java".equals(s.get("name").asText())) {
                courseSubjectId = s.get("id").asLong();
                break;
            }
        }
        assertThat(courseSubjectId).isGreaterThan(0);

        LocalDateTime badStart = LocalDateTime.now().plusDays(2).withHour(11).withMinute(17).withSecond(0).withNano(0);
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        String schedulePayload = "{\"startTime\":\"" + badStart.format(DateTimeFormatter.ofPattern(pattern))
                + "\",\"courseSubjectId\":" + courseSubjectId + "}";
        assertThat(exchangeStatus("POST", "/api/schedules/me", teacherToken, schedulePayload)).isEqualTo(400);
    }

    @Test
    void adjacentThirtyMinuteSlotsDoNotConflict() throws Exception {
        String teacherToken = registerAndGetToken("Teacher Adj", "teacheradj@test.mn", "TEACHER");
        exchange("PUT", "/api/teachers/me", teacherToken, """
                {
                  "headline": "T",
                  "bio": "B",
                  "subjects": ["Java"],
                  "skills": ["REST"],
                  "hourlyRate": 35
                }
                """);
        String subjectsBody = exchange("GET", "/api/course/subjects", null, null);
        JsonNode subjectsArr = objectMapper.readTree(subjectsBody);
        long courseSubjectId = -1;
        for (JsonNode s : subjectsArr) {
            if ("Java".equals(s.get("name").asText())) {
                courseSubjectId = s.get("id").asLong();
                break;
            }
        }
        assertThat(courseSubjectId).isGreaterThan(0);

        LocalDateTime day = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        String first = "{\"startTime\":\"" + day.format(DateTimeFormatter.ofPattern(pattern))
                + "\",\"courseSubjectId\":" + courseSubjectId + "}";
        String second = "{\"startTime\":\"" + day.plusMinutes(30).format(DateTimeFormatter.ofPattern(pattern))
                + "\",\"courseSubjectId\":" + courseSubjectId + "}";

        assertThat(exchangeStatus("POST", "/api/schedules/me", teacherToken, first)).isEqualTo(200);
        assertThat(exchangeStatus("POST", "/api/schedules/me", teacherToken, second)).isEqualTo(200);
    }

    @Test
    void duplicateSameSlotReturns409() throws Exception {
        String teacherToken = registerAndGetToken("Teacher Dup", "teacherdup@test.mn", "TEACHER");
        exchange("PUT", "/api/teachers/me", teacherToken, """
                {
                  "headline": "T",
                  "bio": "B",
                  "subjects": ["Java"],
                  "skills": ["REST"],
                  "hourlyRate": 35
                }
                """);
        String subjectsBody = exchange("GET", "/api/course/subjects", null, null);
        JsonNode subjectsArr = objectMapper.readTree(subjectsBody);
        long courseSubjectId = -1;
        for (JsonNode s : subjectsArr) {
            if ("Java".equals(s.get("name").asText())) {
                courseSubjectId = s.get("id").asLong();
                break;
            }
        }
        assertThat(courseSubjectId).isGreaterThan(0);

        LocalDateTime start = LocalDateTime.now().plusDays(4).withHour(14).withMinute(0).withSecond(0).withNano(0);
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";
        String payload = "{\"startTime\":\"" + start.format(DateTimeFormatter.ofPattern(pattern))
                + "\",\"courseSubjectId\":" + courseSubjectId + "}";

        assertThat(exchangeStatus("POST", "/api/schedules/me", teacherToken, payload)).isEqualTo(200);
        assertThat(exchangeStatus("POST", "/api/schedules/me", teacherToken, payload)).isEqualTo(409);
    }

    private int exchangeStatus(String method, String path, String bearerToken, String jsonBody) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(30));
        if (bearerToken != null) {
            b.header("Authorization", "Bearer " + bearerToken);
        }
        if ("GET".equals(method)) {
            b.GET();
        } else if (jsonBody != null) {
            b.header("Content-Type", "application/json; charset=UTF-8");
            b.method(method, HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> res = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return res.statusCode();
    }

    private String exchange(String method, String path, String bearerToken, String jsonBody) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(30));
        if (bearerToken != null) {
            b.header("Authorization", "Bearer " + bearerToken);
        }
        if ("GET".equals(method)) {
            b.GET();
        } else if (jsonBody != null) {
            b.header("Content-Type", "application/json; charset=UTF-8");
            b.method(method, HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> res = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(res.statusCode()).withFailMessage(res.body()).isBetween(200, 299);
        return res.body();
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

        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/auth/register"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(res.statusCode()).isBetween(200, 299);
        JsonNode json = objectMapper.readTree(res.body());
        return json.get("accessToken").asText();
    }
}
