package com.tsolmon.online_teaching_platform.dashboard.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void dashboardEndpointsShouldReturnOperationalData() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("dashboards");

        JsonNode publicStats = objectMapper.readTree(exchange("GET", "/api/public/stats", null, null));
        assertThat(publicStats.get("totalTeachers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(publicStats.get("totalBookings").asLong()).isGreaterThanOrEqualTo(1);

        JsonNode teacherDashboard = objectMapper.readTree(exchange("GET", "/api/dashboard/teacher", access.teacherToken(), null));
        assertThat(teacherDashboard.get("uniqueStudentsConfirmed").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(teacherDashboard.get("recentBookings").isArray()).isTrue();

        JsonNode studentDashboard = objectMapper.readTree(exchange("GET", "/api/dashboard/student", access.studentToken(), null));
        assertThat(studentDashboard.get("recentBookings").isArray()).isTrue();
        assertThat(studentDashboard.get("unreadNotifications").asLong()).isGreaterThanOrEqualTo(1);
    }
}
