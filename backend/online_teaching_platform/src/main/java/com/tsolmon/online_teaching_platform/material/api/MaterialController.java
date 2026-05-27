package com.tsolmon.online_teaching_platform.material.api;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.material.api.dto.MaterialDownloadUrlResponse;
import com.tsolmon.online_teaching_platform.material.api.dto.TeachingMaterialResponse;
import com.tsolmon.online_teaching_platform.material.application.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;

    @GetMapping("/{materialId}/download-url")
    public MaterialDownloadUrlResponse downloadUrl(
            @PathVariable Long materialId,
            Authentication authentication
    ) {
        AuthUser viewer = (AuthUser) authentication.getPrincipal();
        return materialService.resolveDownloadUrl(materialId, viewer);
    }

    @GetMapping("/{materialId}/download")
    public ResponseEntity<Void> download(
            @PathVariable Long materialId,
            Authentication authentication
    ) {
        AuthUser viewer = (AuthUser) authentication.getPrincipal();
        return materialService.downloadRedirect(materialId, viewer);
    }

    @GetMapping("/teacher/{teacherProfileId}")
    public List<TeachingMaterialResponse> listForTeacher(
            @PathVariable Long teacherProfileId,
            Authentication authentication
    ) {
        AuthUser viewer = authUserOrNull(authentication);
        return materialService.listForTeacher(teacherProfileId, viewer);
    }

    private static AuthUser authUserOrNull(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object p = authentication.getPrincipal();
        return p instanceof AuthUser au ? au : null;
    }

    @PostMapping(consumes = "multipart/form-data")
    public TeachingMaterialResponse upload(
            Authentication authentication,
            @RequestParam("courseSubjectId") Long courseSubjectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description
    ) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        return materialService.upload(authUser, courseSubjectId, title, description, file);
    }

    @DeleteMapping("/{materialId}")
    public void delete(Authentication authentication, @PathVariable Long materialId) {
        AuthUser authUser = (AuthUser) authentication.getPrincipal();
        materialService.delete(authUser, materialId);
    }
}
