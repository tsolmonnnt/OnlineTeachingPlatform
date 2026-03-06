package com.tsolmon.online_teaching_platform.teacher.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.teacher.api.dto.TeacherProfileResponse;
import com.tsolmon.online_teaching_platform.teacher.api.dto.UpdateTeacherProfileRequest;
import com.tsolmon.online_teaching_platform.teacher.application.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
