package com.tsolmon.online_teaching_platform.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import com.tsolmon.online_teaching_platform.user.entity.User;
import org.junit.jupiter.api.Test;

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

        assertThat(exchangeStatus("POST", "/api/auth/register", null, """
                {
                  "fullName": "Duplicate Auth Case",
                  "email": "AUTHCASE@test.mn",
                  "password": "%s",
                  "role": "STUDENT"
                }
                """.formatted(PASSWORD))).isEqualTo(409);
    }
}
