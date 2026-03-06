package com.tsolmon.online_teaching_platform.teacher.application;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherProfileResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.UpdateTeacherProfileRequest;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;

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
        profile.setLanguages(request.languages() == null ? new ArrayList<>() : new ArrayList<>(request.languages()));

        TeacherProfile saved = teacherRepository.save(profile);
        return TeacherProfileResponse.from(saved);
    }
}
