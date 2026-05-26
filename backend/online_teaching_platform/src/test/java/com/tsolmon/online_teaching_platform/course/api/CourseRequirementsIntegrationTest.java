package com.tsolmon.online_teaching_platform.course.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void courseCatalogEndpointsShouldReturnCategoriesAndSubjects() throws Exception {
        JsonNode categories = objectMapper.readTree(exchange("GET", "/api/course/categories", null, null));
        assertThat(categories.isArray()).isTrue();
        assertThat(categories.size()).isGreaterThan(0);
        long firstCategoryId = categories.get(0).get("id").asLong();

        JsonNode subjectsByCategory = objectMapper.readTree(exchange("GET", "/api/course/subjects?categoryId=" + firstCategoryId, null, null));
        assertThat(subjectsByCategory.isArray()).isTrue();
        assertThat(subjectsByCategory.size()).isGreaterThan(0);
    }
}
