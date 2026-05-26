package com.tsolmon.online_teaching_platform.material.application;

import com.cloudinary.Cloudinary;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.application.CourseAccessService;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.material.api.dto.TeachingMaterialResponse;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterialRepository;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryProperties;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private TeachingMaterialRepository materialRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private CourseSubjectRepository courseSubjectRepository;
    @Mock
    private CourseAccessService courseAccessService;
    @Mock
    private Cloudinary cloudinary;

    @Test
    void listForTeacherOnlyIncludesSecureUrlForOwnerOrStudentWithCourseAccess() {
        TeacherProfile owner = teacher(10L, 100L, "Java");
        CourseSubject java = subject(3L, "Java");
        TeachingMaterial material = material(99L, owner, java);
        when(materialRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(material));

        when(teacherRepository.findByUser_Id(100L)).thenReturn(Optional.of(owner));
        List<TeachingMaterialResponse> ownerResponse = service(configured()).listForTeacher(
                10L,
                new AuthUser(100L, "teacher@test.mn", Role.TEACHER)
        );
        assertThat(ownerResponse.get(0).secureUrl()).isEqualTo(material.getSecureUrl());

        List<TeachingMaterialResponse> anonymousResponse = service(configured()).listForTeacher(10L, null);
        assertThat(anonymousResponse.get(0).secureUrl()).isNull();

        when(courseAccessService.hasConfirmedAccess(200L, 10L, 3L)).thenReturn(false, true);
        List<TeachingMaterialResponse> blockedStudent = service(configured()).listForTeacher(
                10L,
                new AuthUser(200L, "student@test.mn", Role.STUDENT)
        );
        List<TeachingMaterialResponse> allowedStudent = service(configured()).listForTeacher(
                10L,
                new AuthUser(200L, "student@test.mn", Role.STUDENT)
        );
        assertThat(blockedStudent.get(0).secureUrl()).isNull();
        assertThat(allowedStudent.get(0).secureUrl()).isEqualTo(material.getSecureUrl());
    }

    @Test
    void uploadValidationRejectsBadInputBeforeRepositoryWork() {
        MaterialService service = service(configured());
        AuthUser teacher = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);

        assertStatus(400, () -> service.upload(teacher, 1L, "T", null, null));
        assertStatus(400, () -> service.upload(teacher, 1L, "T", null,
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])));
        assertStatus(413, () -> service.upload(teacher, 1L, "T", null,
                new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[41 * 1024 * 1024])));
        assertStatus(400, () -> service.upload(teacher, null, "T", null,
                new MockMultipartFile("file", "ok.pdf", "application/pdf", "ok".getBytes())));
    }

    @Test
    void uploadValidationRejectsMissingTeacherUnknownSubjectAndUnofferedSubject() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        MockMultipartFile file = new MockMultipartFile("file", "ok.pdf", "application/pdf", "ok".getBytes());

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service(configured()).upload(authUser, 2L, "T", null, file));

        TeacherProfile teacher = teacher(10L, 1L, "Java");
        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(courseSubjectRepository.findById(2L)).thenReturn(Optional.empty());
        assertStatus(400, () -> service(configured()).upload(authUser, 2L, "T", null, file));

        when(courseSubjectRepository.findById(2L)).thenReturn(Optional.of(subject(2L, "Python")));
        assertStatus(400, () -> service(configured()).upload(authUser, 2L, "T", null, file));
    }

    @Test
    void uploadAndDeleteReturnServiceUnavailableWhenCloudinaryIsNotConfigured() {
        AuthUser teacher = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        MockMultipartFile file = new MockMultipartFile("file", "ok.pdf", "application/pdf", "ok".getBytes());
        assertStatus(503, () -> service(unconfigured()).upload(teacher, 2L, "T", null, file));
    }

    @Test
    void deleteRejectsMissingMaterialMissingTeacherAndForeignMaterial() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);

        when(materialRepository.findById(404L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service(configured()).delete(authUser, 404L));

        TeachingMaterial material = material(20L, teacher(10L, 100L, "Java"), subject(1L, "Java"));
        when(materialRepository.findById(20L)).thenReturn(Optional.of(material));
        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service(configured()).delete(authUser, 20L));

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher(11L, 1L, "Java")));
        assertStatus(403, () -> service(configured()).delete(authUser, 20L));
    }

    private MaterialService service(CloudinaryProperties properties) {
        return new MaterialService(
                materialRepository,
                teacherRepository,
                courseSubjectRepository,
                courseAccessService,
                cloudinary,
                properties
        );
    }

    private static CloudinaryProperties configured() {
        return new CloudinaryProperties("cloud", "key", "secret", null, null, null, null);
    }

    private static CloudinaryProperties unconfigured() {
        return new CloudinaryProperties("", "", "", null, null, null, null);
    }

    private static TeacherProfile teacher(Long profileId, Long userId, String subject) {
        User user = new User();
        user.setId(userId);
        user.setFullName("Teacher " + userId);
        user.setEmail("teacher" + userId + "@test.mn");
        user.setRole(Role.TEACHER);

        TeacherProfile teacher = new TeacherProfile();
        teacher.setId(profileId);
        teacher.setUser(user);
        teacher.setSubjects(List.of(subject));
        return teacher;
    }

    private static CourseSubject subject(Long id, String name) {
        CourseSubject subject = new CourseSubject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }

    private static TeachingMaterial material(Long id, TeacherProfile teacher, CourseSubject subject) {
        TeachingMaterial material = new TeachingMaterial();
        material.setId(id);
        material.setTeacherProfile(teacher);
        material.setCourseSubject(subject);
        material.setTitle("Material");
        material.setCloudinaryPublicId("public-id");
        material.setSecureUrl("https://res.cloudinary.com/demo/raw/upload/material.pdf");
        return material;
    }

    private static void assertStatus(int status, ThrowingRunnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(status));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
