package com.tsolmon.online_teaching_platform.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import com.tsolmon.online_teaching_platform.user.entity.User;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void fr01Fr02AndNfr01AuthenticationShouldRegisterHashAndLoginUsers() throws Exception {
        String email = "AuthCase@test.mn";
        String registerBody = exchange("POST", "/api/auth/register", null, """
                {
                  "fullName": "Auth Case",
                  "email": "%s",
                  "password": "%s",
                  "role": "STUDENT"
                }
                """.formatted(email, PASSWORD));
        JsonNode registered = objectMapper.readTree(registerBody);

        assertThat(registered.get("accessToken").asText()).isNotBlank();
        assertThat(registered.get("user").get("email").asText()).isEqualTo("authcase@test.mn");

        User saved = userRepository.findByEmail("authcase@test.mn").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(saved.getPassword()).startsWith("$2");

        String loginBody = exchange("POST", "/api/auth/login", null, """
                {
                  "email": "authcase@test.mn",
                  "password": "%s"
                }
                """.formatted(PASSWORD));
        assertThat(objectMapper.readTree(loginBody).get("accessToken").asText()).isNotBlank();

        assertThat(exchangeStatus("POST", "/api/auth/login", null, """
                {
                  "email": "authcase@test.mn",
                  "password": "wrong-password"
                }
                """)).isEqualTo(401);

        HttpResponse<String> duplicateRegister = send("POST", "/api/auth/register", null, """
                {
                  "fullName": "Duplicate Auth Case",
                  "email": "AUTHCASE@test.mn",
                  "password": "%s",
                  "role": "STUDENT"
                }
                """.formatted(PASSWORD));
        assertThat(duplicateRegister.statusCode()).isEqualTo(409);

        JsonNode duplicateError = objectMapper.readTree(duplicateRegister.body());
        assertThat(duplicateError.get("code").asText()).isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(duplicateError.get("message").asText()).isEqualTo("Email already registered");
        assertThat(duplicateError.get("fieldErrors").isArray()).isTrue();
    }

    @Test
    void registerValidationErrorsShouldReturnStructuredFieldErrors() throws Exception {
        HttpResponse<String> invalidRegister = send("POST", "/api/auth/register", null, """
                {
                  "fullName": "",
                  "email": "not-an-email",
                  "password": "123",
                  "role": "STUDENT"
                }
                """);

        assertThat(invalidRegister.statusCode()).isEqualTo(400);

        JsonNode error = objectMapper.readTree(invalidRegister.body());
        assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.get("message").asText()).isEqualTo("Validation failed");

        JsonNode fieldErrors = error.get("fieldErrors");
        assertThat(fieldErrors.isArray()).isTrue();

        var fields = StreamSupport.stream(fieldErrors.spliterator(), false)
                .map(node -> node.get("field").asText())
                .toList();
        var codes = StreamSupport.stream(fieldErrors.spliterator(), false)
                .map(node -> node.get("code").asText())
                .toList();

        assertThat(fields).contains("fullName", "email", "password");
        assertThat(codes).contains("REQUIRED", "INVALID_EMAIL", "PASSWORD_LENGTH");
    }

    @Test
    void patchMeShouldUpdateFullNameForAuthenticatedUser() throws Exception {
        RegisteredUser user = registerUser("Original Name", "patch.me@test.mn", "STUDENT");

        String patchBody = exchange("PATCH", "/api/auth/me", user.token(), """
                {
                  "fullName": "Шинэчилсэн нэр"
                }
                """);
        assertThat(objectMapper.readTree(patchBody).get("fullName").asText()).isEqualTo("Шинэчилсэн нэр");

        String meBody = exchange("GET", "/api/auth/me", user.token(), null);
        assertThat(objectMapper.readTree(meBody).get("fullName").asText()).isEqualTo("Шинэчилсэн нэр");
    }

    @Test
    void meShouldExposeAvatarUrlAndAvatarUploadRequiresCloudinary() throws Exception {
        RegisteredUser user = registerUser("Avatar User", "avatar.user@test.mn", "STUDENT");

        String meBody = exchange("GET", "/api/auth/me", user.token(), null);
        JsonNode me = objectMapper.readTree(meBody);
        assertThat(me.has("avatarUrl")).isTrue();
        assertThat(me.get("avatarUrl").isNull()).isTrue();

        HttpResponse<String> upload = sendMultipart(
                "POST",
                "/api/auth/me/avatar",
                user.token(),
                List.of(),
                "file",
                "avatar.png",
                "image/png",
                "fake-image-bytes"
        );
        assertThat(upload.statusCode()).isEqualTo(503);
    }
}
