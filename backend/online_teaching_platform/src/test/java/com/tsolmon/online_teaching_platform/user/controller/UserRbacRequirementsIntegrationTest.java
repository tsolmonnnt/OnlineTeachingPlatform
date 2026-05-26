package com.tsolmon.online_teaching_platform.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRbacRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void fr03AndNfr01RbacShouldProtectAdminAndLetAdminManageUserRoles() throws Exception {
        RegisteredUser student = registerUser("Rbac Student", "rbac.student@test.mn", "STUDENT");
        RegisteredUser admin = registerUser("Rbac Admin", "rbac.admin@test.mn", "ADMIN");

        assertThat(exchangeStatus("GET", "/api/users", null, null)).isEqualTo(403);
        assertThat(exchangeStatus("GET", "/api/users", student.token(), null)).isEqualTo(403);

        String usersBody = exchange("GET", "/api/users", admin.token(), null);
        assertThat(objectMapper.readTree(usersBody).isArray()).isTrue();

        String patchedBody = exchange("PATCH", "/api/users/" + student.userId() + "/role", admin.token(), """
                {
                  "role": "TEACHER"
                }
                """);
        JsonNode patched = objectMapper.readTree(patchedBody);
        assertThat(patched.get("id").asLong()).isEqualTo(student.userId());
        assertThat(patched.get("role").asText()).isEqualTo("TEACHER");
    }
}
