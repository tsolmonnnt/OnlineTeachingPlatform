package com.tsolmon.online_teaching_platform.dashboard.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.dashboard.api.dto.StudentDashboardResponse;
import com.tsolmon.online_teaching_platform.dashboard.api.dto.TeacherDashboardResponse;
import com.tsolmon.online_teaching_platform.dashboard.application.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/teacher")
    public TeacherDashboardResponse teacher(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return dashboardService.getTeacherDashboard(authUser);
    }

    @GetMapping("/student")
    public StudentDashboardResponse student(Authentication authentication) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return dashboardService.getStudentDashboard(authUser);
    }
}
