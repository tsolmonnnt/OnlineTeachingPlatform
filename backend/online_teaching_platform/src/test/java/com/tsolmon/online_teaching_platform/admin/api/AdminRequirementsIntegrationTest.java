package com.tsolmon.online_teaching_platform.admin.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void adminEndpointsShouldReturnStatsListTeachersAndPatchVerification() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("admin");
        RegisteredUser admin = registerUser("Dashboard Admin", "dashboard.admin@test.mn", "ADMIN");

        JsonNode adminStats = objectMapper.readTree(exchange("GET", "/api/admin/stats", admin.token(), null));
        assertThat(adminStats.get("totalUsers").asLong()).isGreaterThanOrEqualTo(3);

        JsonNode adminTeachers = objectMapper.readTree(exchange("GET", "/api/admin/teachers", admin.token(), null));
        assertThat(adminTeachers.isArray()).isTrue();

        JsonNode verifiedTeacher = objectMapper.readTree(exchange(
                "PATCH",
                "/api/admin/teachers/" + access.teacherProfileId() + "/verification",
                admin.token(),
                """
                        {
                          "verified": true
                        }
                        """
        ));
        assertThat(verifiedTeacher.get("verified").asBoolean()).isTrue();
    }
}
