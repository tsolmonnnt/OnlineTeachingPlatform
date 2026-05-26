package com.tsolmon.online_teaching_platform.teacher.application;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.course.application.CourseCatalogService;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryProperties;
import com.tsolmon.online_teaching_platform.review.domain.ReviewRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilityRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilitySlot;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherSummaryResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.UpdateTeacherProfileRequest;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private TeacherAvailabilityRepository availabilityRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private CourseCatalogService courseCatalogService;
    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    @Test
    void updateMyProfileDefaultsNullListsAndEnsuresSavedSubjects() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");
        UpdateTeacherProfileRequest request = new UpdateTeacherProfileRequest(
                "Senior mentor",
                "Clear Java lessons",
                null,
                null,
                "https://example.test/avatar.png",
                BigDecimal.valueOf(25),
                null,
                "Ulaanbaatar",
                "99112233",
                6
        );

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(teacher)).thenReturn(teacher);

        assertThat(service(configured()).updateMyProfile(authUser, request).headline()).isEqualTo("Senior mentor");

        assertThat(teacher.getSubjects()).isEmpty();
        assertThat(teacher.getSkills()).isEmpty();
        assertThat(teacher.getLanguages()).isEmpty();
        verify(courseCatalogService).ensureSubjectsExistForStrings(List.of());
    }

    @Test
    void searchTeachersCoversTextListAndAvailabilityFilters() {
        TeacherProfile javaTeacher = teacher(10L, 1L, "Java");
        javaTeacher.setHeadline(null);
        javaTeacher.setBio(null);
        List<String> javaSkills = new ArrayList<>();
        javaSkills.add("Spring");
        javaSkills.add(null);
        javaTeacher.setSkills(javaSkills);

        TeacherProfile pythonTeacher = teacher(11L, 2L, "Python");
        pythonTeacher.getUser().setFullName(null);
        pythonTeacher.setHeadline("Data mentor");
        pythonTeacher.setBio("Machine learning");
        pythonTeacher.setSkills(List.of("Pandas"));

        TeacherProfile withoutUser = teacher(12L, 3L, "Java");
        withoutUser.setUser(null);

        when(teacherRepository.findAll()).thenReturn(List.of(javaTeacher, pythonTeacher, withoutUser));
        when(reviewRepository.averageRatingByTeacher(anyLong())).thenReturn(Optional.empty());

        List<TeacherSummaryResponse> noFilters = service(configured()).searchTeachers("  ", null, null, null);
        assertThat(noFilters).extracting(TeacherSummaryResponse::id).containsExactly(10L, 11L);

        List<TeacherSummaryResponse> subjectAndSkill = service(configured()).searchTeachers(
                "java",
                "java",
                "spring",
                null
        );
        assertThat(subjectAndSkill).extracting(TeacherSummaryResponse::id).containsExactly(10L);

        LocalDateTime after = LocalDateTime.of(2026, 6, 1, 10, 0);
        when(availabilityRepository.findByTeacherProfile_IdOrderByStartTimeAsc(10L))
                .thenReturn(List.of(
                        slot(1L, javaTeacher, after.minusHours(1), true),
                        slot(2L, javaTeacher, after.minusMinutes(30), false),
                        slot(3L, javaTeacher, after.plusMinutes(30), false)
                ));
        when(availabilityRepository.findByTeacherProfile_IdOrderByStartTimeAsc(11L))
                .thenReturn(List.of(slot(4L, pythonTeacher, after.plusMinutes(30), true)));

        List<TeacherSummaryResponse> available = service(configured()).searchTeachers(null, null, null, after);
        assertThat(available).extracting(TeacherSummaryResponse::id).containsExactly(10L);
    }

    @Test
    void uploadAvatarRejectsValidationFailuresBeforeUpload() {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherService service = service(configured());

        assertStatus(503, () -> service(unconfigured()).uploadAvatar(authUser, image("avatar.png", "image/png")));
        assertStatus(400, () -> service.uploadAvatar(authUser, null));
        assertStatus(400, () -> service.uploadAvatar(authUser,
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])));
        assertStatus(413, () -> service.uploadAvatar(authUser,
                new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1])));
        assertStatus(400, () -> service.uploadAvatar(authUser,
                new MockMultipartFile("file", "avatar.bin", null, "bytes".getBytes())));
        assertStatus(400, () -> service.uploadAvatar(authUser,
                new MockMultipartFile("file", "avatar.txt", "text/plain", "bytes".getBytes())));

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertStatus(404, () -> service.uploadAvatar(authUser, image("avatar.png", "image/png")));
    }

    @Test
    void uploadAvatarStoresReturnedUrlAndPassesPresetWhenConfigured() throws IOException {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");
        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), options.capture())).thenReturn(Map.of("secure_url", "https://cdn.test/avatar.png"));
        when(teacherRepository.save(teacher)).thenReturn(teacher);

        assertThat(service(configuredWithProfilePreset()).uploadAvatar(authUser, image("avatar.png", "image/png")).avatarUrl())
                .isEqualTo("https://cdn.test/avatar.png");

        assertThat(options.getValue())
                .containsEntry("folder", "profiles")
                .containsEntry("resource_type", "image")
                .containsEntry("overwrite", Boolean.FALSE)
                .containsEntry("upload_preset", "profile-preset");
    }

    @Test
    void uploadAvatarMapsMissingUrlAndIoFailureToBadGateway() throws IOException {
        AuthUser authUser = new AuthUser(1L, "teacher@test.mn", Role.TEACHER);
        TeacherProfile teacher = teacher(10L, 1L, "Java");

        when(teacherRepository.findByUser_Id(1L)).thenReturn(Optional.of(teacher));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), any())).thenReturn(Map.of("secure_url", " "));
        assertStatus(502, () -> service(configured()).uploadAvatar(authUser, image("avatar.png", "image/png")));

        when(uploader.upload(any(), any())).thenThrow(new IOException("network"));
        assertStatus(502, () -> service(configured()).uploadAvatar(authUser, image("avatar.png", "image/png")));
    }

    private TeacherService service(CloudinaryProperties properties) {
        return new TeacherService(
                teacherRepository,
                availabilityRepository,
                reviewRepository,
                courseCatalogService,
                cloudinary,
                properties
        );
    }

    private static CloudinaryProperties configured() {
        return new CloudinaryProperties("cloud", "key", "secret", null, null, null, null);
    }

    private static CloudinaryProperties configuredWithProfilePreset() {
        return new CloudinaryProperties("cloud", "key", "secret", null, null, null, "profile-preset");
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
        List<String> subjects = new ArrayList<>();
        subjects.add(subject);
        subjects.add(null);
        teacher.setSubjects(subjects);
        teacher.setSkills(List.of());
        return teacher;
    }

    private static TeacherAvailabilitySlot slot(Long id, TeacherProfile teacher, LocalDateTime startTime, boolean booked) {
        TeacherAvailabilitySlot slot = new TeacherAvailabilitySlot();
        slot.setId(id);
        slot.setTeacherProfile(teacher);
        slot.setStartTime(startTime);
        slot.setEndTime(startTime.plusMinutes(30));
        slot.setBooked(booked);
        return slot;
    }

    private static MockMultipartFile image(String name, String contentType) {
        return new MockMultipartFile("file", name, contentType, "image-bytes".getBytes());
    }

    private static void assertStatus(int status, ThrowingRunnable runnable) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(status));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
