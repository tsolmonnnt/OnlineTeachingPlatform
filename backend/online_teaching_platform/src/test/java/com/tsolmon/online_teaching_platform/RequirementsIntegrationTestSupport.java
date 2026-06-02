package com.tsolmon.online_teaching_platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterialRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilityRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class RequirementsIntegrationTestSupport {
    protected static final String PASSWORD = "Password123!";
    protected static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    protected int port;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TeacherRepository teacherRepository;

    @Autowired
    protected CourseSubjectRepository courseSubjectRepository;

    @Autowired
    protected TeachingMaterialRepository materialRepository;

    @Autowired
    protected TeacherAvailabilityRepository availabilityRepository;

    protected ConfirmedCourseAccess createConfirmedCourseAccess(String suffix) throws Exception {
        RegisteredUser teacher = registerUser("Teacher " + suffix, suffix + ".teacher@test.mn", "TEACHER");
        RegisteredUser student = registerUser("Student " + suffix, suffix + ".student@test.mn", "STUDENT");

        updateTeacherProfile(teacher.token(), "Java mentor " + suffix, "Java");

        JsonNode teacherArray = objectMapper.readTree(exchange(
                "GET",
                "/api/teachers?query=" + URLEncoder.encode("Java mentor " + suffix, StandardCharsets.UTF_8),
                null,
                null
        ));
        long teacherProfileId = teacherArray.get(0).get("id").asLong();
        CourseSubject java = courseSubjectRepository.findByNameIgnoreCase("Java").orElseThrow();

        LocalDateTime start = LocalDateTime.now()
                .plusDays(10)
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plusDays(suffix.length());
        String slotBody = exchange("POST", "/api/schedules/me", teacher.token(), """
                {
                  "startTime": "%s",
                  "courseSubjectId": %d
                }
                """.formatted(start.format(ISO_SECONDS), java.getId()));
        long slotId = objectMapper.readTree(slotBody).get("id").asLong();

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

        TeacherProfile teacherProfile = teacherRepository.findById(teacherProfileId).orElseThrow();
        return new ConfirmedCourseAccess(
                teacher.token(),
                student.token(),
                student.userId(),
                teacherProfileId,
                bookingId,
                slotId,
                java.getId(),
                teacherProfile,
                java
        );
    }

    /** Moves a booked slot into the recent past so the lesson counts as attended within the access window. */
    protected void attendBookingNow(long slotId) {
        TeacherAvailabilitySlot slot = availabilityRepository.findById(slotId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        slot.setStartTime(now.minusHours(2));
        slot.setEndTime(now.minusHours(1));
        availabilityRepository.save(slot);
    }

    /** Moves a booked slot to a fixed start/end (used by access-window tests). */
    protected void setBookingSlotTimes(long slotId, LocalDateTime start, LocalDateTime end) {
        TeacherAvailabilitySlot slot = availabilityRepository.findById(slotId).orElseThrow();
        slot.setStartTime(start);
        slot.setEndTime(end);
        availabilityRepository.save(slot);
    }

    protected long findTeacherProfileIdByQuery(String query) throws Exception {
        JsonNode teacherArray = objectMapper.readTree(exchange(
                "GET",
                "/api/teachers?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
                null,
                null
        ));
        assertThat(teacherArray.size()).isGreaterThan(0);
        return teacherArray.get(0).get("id").asLong();
    }

    protected long createSlot(String teacherToken, long courseSubjectId, LocalDateTime start) throws Exception {
        String slotBody = exchange("POST", "/api/schedules/me", teacherToken, """
                {
                  "startTime": "%s",
                  "courseSubjectId": %d
                }
                """.formatted(start.format(ISO_SECONDS), courseSubjectId));
        return objectMapper.readTree(slotBody).get("id").asLong();
    }

    protected void updateTeacherProfile(String teacherToken, String headline, String subject) throws Exception {
        exchange("PUT", "/api/teachers/me", teacherToken, """
                {
                  "headline": "%s",
                  "bio": "Backend focus",
                  "subjects": ["%s"],
                  "skills": ["REST", "SQL"],
                  "hourlyRate": 35
                }
                """.formatted(headline, subject));
    }

    protected RegisteredUser registerUser(String fullName, String email, String role) throws Exception {
        String body = exchange("POST", "/api/auth/register", null, """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """.formatted(fullName, email, PASSWORD, role));
        JsonNode json = objectMapper.readTree(body);
        return new RegisteredUser(json.get("accessToken").asText(), json.get("user").get("id").asLong());
    }

    protected int exchangeStatus(String method, String path, String bearerToken, String jsonBody) throws Exception {
        HttpResponse<String> res = send(method, path, bearerToken, jsonBody);
        return res.statusCode();
    }

    protected String exchange(String method, String path, String bearerToken, String jsonBody) throws Exception {
        HttpResponse<String> res = send(method, path, bearerToken, jsonBody);
        assertThat(res.statusCode()).withFailMessage(res.body()).isBetween(200, 299);
        return res.body();
    }

    protected HttpResponse<String> send(String method, String path, String bearerToken, String jsonBody) throws Exception {
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
        return httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    protected HttpResponse<String> sendMultipart(
            String method,
            String path,
            String bearerToken,
            List<MultipartField> fields,
            String fileField,
            String fileName,
            String contentType,
            String fileContent
    ) throws Exception {
        String boundary = "----otp-test-boundary";
        StringBuilder body = new StringBuilder();
        for (MultipartField field : fields) {
            body.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"").append(field.name()).append("\"\r\n\r\n")
                    .append(field.value()).append("\r\n");
        }
        body.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(fileField)
                .append("\"; filename=\"").append(fileName).append("\"\r\n")
                .append("Content-Type: ").append(contentType).append("\r\n\r\n")
                .append(fileContent).append("\r\n")
                .append("--").append(boundary).append("--\r\n");

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (bearerToken != null) {
            b.header("Authorization", "Bearer " + bearerToken);
        }
        b.method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        return httpClient.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    public record RegisteredUser(String token, long userId) {
    }

    public record ConfirmedCourseAccess(
            String teacherToken,
            String studentToken,
            long studentUserId,
            long teacherProfileId,
            long bookingId,
            long slotId,
            long courseSubjectId,
            TeacherProfile teacherProfile,
            CourseSubject courseSubject
    ) {
    }

    public record MultipartField(String name, String value) {
    }
}
