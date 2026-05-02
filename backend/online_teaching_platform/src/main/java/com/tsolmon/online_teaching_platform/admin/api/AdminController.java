package com.tsolmon.online_teaching_platform.admin.api;

import com.tsolmon.online_teaching_platform.admin.api.dto.AdminStatsResponse;
import com.tsolmon.online_teaching_platform.admin.api.dto.AdminTeacherRowResponse;
import com.tsolmon.online_teaching_platform.admin.api.dto.PatchTeacherVerificationRequest;
import com.tsolmon.online_teaching_platform.admin.application.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.getStats();
    }

    @GetMapping("/teachers")
    public List<AdminTeacherRowResponse> listTeachers() {
        return adminService.listTeachers();
    }

    @PatchMapping("/teachers/{teacherProfileId}/verification")
    public AdminTeacherRowResponse patchVerification(
            @PathVariable Long teacherProfileId,
            @Valid @RequestBody PatchTeacherVerificationRequest request
    ) {
        return adminService.setTeacherVerified(teacherProfileId, request.verified());
    }
}
