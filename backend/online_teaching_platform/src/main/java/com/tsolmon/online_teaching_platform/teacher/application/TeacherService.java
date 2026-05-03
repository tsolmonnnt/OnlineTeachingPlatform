package com.tsolmon.online_teaching_platform.teacher.application;

import com.cloudinary.Cloudinary;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.course.application.CourseCatalogService;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryProperties;
import com.tsolmon.online_teaching_platform.review.domain.ReviewRepository;
import com.tsolmon.online_teaching_platform.schedule.domain.TeacherAvailabilityRepository;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherDetailResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherProfileResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherSummaryResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.UpdateTeacherProfileRequest;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;

    private final TeacherRepository teacherRepository;
    private final TeacherAvailabilityRepository availabilityRepository;
    private final ReviewRepository reviewRepository;
    private final CourseCatalogService courseCatalogService;
    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Transactional(readOnly = true)
    public TeacherProfileResponse getMyProfile(AuthUser authUser) {
        TeacherProfile profile = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        return TeacherProfileResponse.from(profile);
    }

    @Transactional
    public TeacherProfileResponse updateMyProfile(AuthUser authUser, UpdateTeacherProfileRequest request) {
        TeacherProfile profile = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));

        profile.setHeadline(request.headline());
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setHourlyRate(request.hourlyRate());
        profile.setLocation(request.location());
        profile.setPhone(request.phone());
        profile.setYearsExperience(request.yearsExperience());

        profile.setSubjects(request.subjects() == null ? new ArrayList<>() : new ArrayList<>(request.subjects()));
        profile.setSkills(request.skills() == null ? new ArrayList<>() : new ArrayList<>(request.skills()));
        profile.setLanguages(request.languages() == null ? new ArrayList<>() : new ArrayList<>(request.languages()));

        TeacherProfile saved = teacherRepository.save(profile);
        courseCatalogService.ensureSubjectsExistForStrings(saved.getSubjects());
        return TeacherProfileResponse.from(saved);
    }

    /**
     * Uploads a profile image to Cloudinary (folder {@link CloudinaryProperties#effectiveProfileFolder()})
     * and stores {@code secure_url} on the teacher profile.
     */
    @Transactional
    public TeacherProfileResponse uploadAvatar(AuthUser authUser, MultipartFile file) {
        if (!cloudinaryProperties.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Image upload is not configured (set CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET)"
            );
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image too large (max 5MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required");
        }

        TeacherProfile profile = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));

        try {
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("folder", cloudinaryProperties.effectiveProfileFolder());
            uploadOptions.put("resource_type", "image");
            uploadOptions.put("overwrite", Boolean.FALSE);
            String preset = cloudinaryProperties.effectiveProfileUploadPreset();
            if (!preset.isBlank()) {
                uploadOptions.put("upload_preset", preset);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upload did not return a URL");
            }
            profile.setAvatarUrl(secureUrl);
            TeacherProfile saved = teacherRepository.save(profile);
            return TeacherProfileResponse.from(saved);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload image", e);
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherSummaryResponse> searchTeachers(String query, String subject, String skill, LocalDateTime availableAfter) {
        String normalizedQuery = normalize(query);
        String normalizedSubject = normalize(subject);
        String normalizedSkill = normalize(skill);

        return teacherRepository.findAll().stream()
                .filter(p -> p.getUser() != null)
                .filter(p -> matchesText(p, normalizedQuery))
                .filter(p -> matchesList(p.getSubjects(), normalizedSubject))
                .filter(p -> matchesList(p.getSkills(), normalizedSkill))
                .filter(p -> hasAvailabilityAfter(p.getId(), availableAfter))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherDetailResponse getTeacherById(Long teacherId) {
        TeacherProfile profile = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        return TeacherDetailResponse.from(
                profile,
                reviewRepository.averageRatingByTeacher(profile.getId()).orElse(null),
                reviewRepository.countByTeacherProfile_Id(profile.getId())
        );
    }

    private TeacherSummaryResponse toSummary(TeacherProfile profile) {
        return TeacherSummaryResponse.from(
                profile,
                reviewRepository.averageRatingByTeacher(profile.getId()).orElse(null),
                reviewRepository.countByTeacherProfile_Id(profile.getId())
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static boolean matchesText(TeacherProfile profile, String query) {
        if (query == null) {
            return true;
        }

        String name = profile.getUser().getFullName();
        String headline = profile.getHeadline();
        String bio = profile.getBio();

        return containsIgnoreCase(name, query)
                || containsIgnoreCase(headline, query)
                || containsIgnoreCase(bio, query)
                || profile.getSubjects().stream().filter(Objects::nonNull).anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(query))
                || profile.getSkills().stream().filter(Objects::nonNull).anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(query));
    }

    private static boolean matchesList(List<String> values, String target) {
        if (target == null) {
            return true;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(target));
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean hasAvailabilityAfter(Long teacherProfileId, LocalDateTime availableAfter) {
        if (availableAfter == null) {
            return true;
        }
        return availabilityRepository.findByTeacherProfile_IdOrderByStartTimeAsc(teacherProfileId).stream()
                .anyMatch(slot -> !slot.isBooked() && !slot.getStartTime().isBefore(availableAfter));
    }
}
