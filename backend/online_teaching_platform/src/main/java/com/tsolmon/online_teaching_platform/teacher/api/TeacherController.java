package com.tsolmon.online_teaching_platform.teacher.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherDetailResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherProfileResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherSummaryResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.UpdateTeacherProfileRequest;
import com.tsolmon.online_teaching_platform.teacher.application.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    @GetMapping("/me")
    public TeacherProfileResponse me(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return teacherService.getMyProfile(authUser);
    }

    @PutMapping("/me")
    public TeacherProfileResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateTeacherProfileRequest request
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return teacherService.updateMyProfile(authUser, request);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeacherProfileResponse uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return teacherService.uploadAvatar(authUser, file);
    }

    @GetMapping
    public List<TeacherSummaryResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) LocalDateTime availableAfter
    ) {
        return teacherService.searchTeachers(query, subject, skill, availableAfter);
    }

    @GetMapping("/{teacherId}")
    public TeacherDetailResponse detail(@PathVariable Long teacherId) {
        return teacherService.getTeacherById(teacherId);
    }
}
