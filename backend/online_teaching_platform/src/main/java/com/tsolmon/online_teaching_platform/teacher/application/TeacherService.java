package com.tsolmon.online_teaching_platform.teacher.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final TeacherAvailabilityRepository availabilityRepository;

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
        return TeacherProfileResponse.from(saved);
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
                .map(TeacherSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherDetailResponse getTeacherById(Long teacherId) {
        TeacherProfile profile = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        return TeacherDetailResponse.from(profile);
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
