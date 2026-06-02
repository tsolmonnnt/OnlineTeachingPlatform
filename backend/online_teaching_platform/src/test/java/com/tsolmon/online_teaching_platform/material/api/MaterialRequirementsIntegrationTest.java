package com.tsolmon.online_teaching_platform.material.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsolmon.online_teaching_platform.RequirementsIntegrationTestSupport;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialRequirementsIntegrationTest extends RequirementsIntegrationTestSupport {

    @Test
    void fr13MaterialsShouldHideCloudinaryUrlUntilStudentHasConfirmedCourseAccess() throws Exception {
        ConfirmedCourseAccess access = createConfirmedCourseAccess("materials");
        TeachingMaterial material = new TeachingMaterial();
        material.setTeacherProfile(access.teacherProfile());
        material.setCourseSubject(access.courseSubject());
        material.setTitle("Java Slides");
        material.setDescription("Lesson notes");
        material.setCloudinaryPublicId("test/java-slides");
        material.setSecureUrl("https://res.cloudinary.com/demo/raw/upload/test/java-slides.pdf");
        material.setContentType("application/pdf");
        material.setSizeBytes(1234L);
        materialRepository.save(material);

        String anonymousBody = exchange("GET", "/api/materials/teacher/" + access.teacherProfileId(), null, null);
        JsonNode anonymous = objectMapper.readTree(anonymousBody);
        assertThat(anonymous).hasSize(1);
        assertThat(anonymous.get(0).get("secureUrl").isNull()).isTrue();

        RegisteredUser otherStudent = registerUser("Other Materials Student", "materials.other.student@test.mn", "STUDENT");
        String noAccessBody = exchange("GET", "/api/materials/teacher/" + access.teacherProfileId(), otherStudent.token(), null);
        assertThat(objectMapper.readTree(noAccessBody).get(0).get("secureUrl").isNull()).isTrue();

        String ownerBody = exchange("GET", "/api/materials/teacher/" + access.teacherProfileId(), access.teacherToken(), null);
        assertThat(objectMapper.readTree(ownerBody).get(0).get("secureUrl").asText()).contains("cloudinary.com");

        // Before the lesson has started, the confirmed booking alone does not grant access.
        String beforeLessonBody = exchange("GET", "/api/materials/teacher/" + access.teacherProfileId(), access.studentToken(), null);
        assertThat(objectMapper.readTree(beforeLessonBody).get(0).get("secureUrl").isNull()).isTrue();

        // Once the lesson has been attended (within the window), the student gets access.
        attendBookingNow(access.slotId());
        String studentBody = exchange("GET", "/api/materials/teacher/" + access.teacherProfileId(), access.studentToken(), null);
        assertThat(objectMapper.readTree(studentBody).get(0).get("secureUrl").asText()).contains("cloudinary.com");
    }

    @Test
    void materialManagementShouldRejectUnconfiguredUploadAndEnforceTeacherOwnershipOnDelete() throws Exception {
        RegisteredUser teacher = registerUser("Material Teacher", "material.manage.teacher@test.mn", "TEACHER");
        RegisteredUser otherTeacher = registerUser("Material Other Teacher", "material.manage.other.teacher@test.mn", "TEACHER");

        updateTeacherProfile(teacher.token(), "Material Java Mentor", "Java");
        updateTeacherProfile(otherTeacher.token(), "Material Other Mentor", "Java");
        long teacherProfileId = findTeacherProfileIdByQuery("Material Java Mentor");
        CourseSubject java = courseSubjectRepository.findByNameIgnoreCase("Java").orElseThrow();

        assertThat(sendMultipart(
                "POST",
                "/api/materials",
                teacher.token(),
                List.of(
                        new MultipartField("courseSubjectId", Long.toString(java.getId())),
                        new MultipartField("title", "Coverage Upload")
                ),
                "file",
                "coverage.pdf",
                "application/pdf",
                "fake pdf"
        ).statusCode()).isEqualTo(503);

        TeachingMaterial material = new TeachingMaterial();
        material.setTeacherProfile(teacherRepository.findById(teacherProfileId).orElseThrow());
        material.setCourseSubject(java);
        material.setTitle("Coverage Material");
        material.setDescription("Temporary material");
        material.setCloudinaryPublicId("coverage/material");
        material.setSecureUrl("https://res.cloudinary.com/demo/raw/upload/coverage/material.pdf");
        material.setContentType("application/pdf");
        material.setSizeBytes(99L);
        long materialId = materialRepository.save(material).getId();

        assertThat(exchangeStatus("DELETE", "/api/materials/" + materialId, otherTeacher.token(), null)).isEqualTo(403);
        assertThat(exchangeStatus("DELETE", "/api/materials/" + materialId, teacher.token(), null)).isBetween(200, 299);
        assertThat(exchangeStatus("DELETE", "/api/materials/999999", teacher.token(), null)).isEqualTo(404);
    }
}
