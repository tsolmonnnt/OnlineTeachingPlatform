package com.tsolmon.online_teaching_platform.teacher.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void nfr03TeacherSearchShouldTreatSqlInjectionTextAsPlainSearchInput() throws Exception {
        RegisteredUser teacher = registerUser("Needle Teacher", "sql.needle.teacher@test.mn", "TEACHER");
        updateTeacherProfile(teacher.token(), "Needle Java Mentor", "Java");

        String normalBody = exchange("GET", "/api/teachers?query=Needle", null, null);
        assertThat(objectMapper.readTree(normalBody).size()).isGreaterThanOrEqualTo(1);

        String injection = URLEncoder.encode("' OR 1=1 --", StandardCharsets.UTF_8);
        String injectionBody = exchange("GET", "/api/teachers?query=" + injection, null, null);
        assertThat(objectMapper.readTree(injectionBody)).isEmpty();
    }

    @Test
    void teacherProfileEndpointsShouldReturnProfileDataAndRejectUnconfiguredAvatarUpload() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("teacherprofile");

        JsonNode teacherMe = objectMapper.readTree(exchange("GET", "/api/teachers/me", access.teacherToken(), null));
        assertThat(teacherMe.get("subjects").get(0).asText()).isEqualTo("Java");

        JsonNode teacherDetail = objectMapper.readTree(exchange("GET", "/api/teachers/" + access.teacherProfileId(), null, null));
        assertThat(teacherDetail.get("id").asLong()).isEqualTo(access.teacherProfileId());

        assertThat(sendMultipart(
                "POST",
                "/api/teachers/me/avatar",
                access.teacherToken(),
                List.of(),
                "file",
                "avatar.txt",
                "text/plain",
                "not-an-image"
        ).statusCode()).isEqualTo(503);
    }
}
