package com.tsolmon.online_teaching_platform.material.api.dto;

import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;

import java.time.LocalDateTime;

public record TeachingMaterialResponse(
        Long id,
        Long teacherProfileId,
        Long courseSubjectId,
        String courseSubjectName,
        String title,
        String description,
        String secureUrl,
        String contentType,
        Long sizeBytes,
        LocalDateTime createdAt
) {
    public static TeachingMaterialResponse from(TeachingMaterial m, boolean includeSecureUrl) {
        Long csId = m.getCourseSubject() != null ? m.getCourseSubject().getId() : null;
        String csName = m.getCourseSubject() != null ? m.getCourseSubject().getName() : null;
        return new TeachingMaterialResponse(
                m.getId(),
                m.getTeacherProfile().getId(),
                csId,
                csName,
                m.getTitle(),
                m.getDescription(),
                includeSecureUrl ? m.getSecureUrl() : null,
                m.getContentType(),
                m.getSizeBytes(),
                m.getCreatedAt()
        );
    }
}
