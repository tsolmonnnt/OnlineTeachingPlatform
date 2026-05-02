package com.tsolmon.online_teaching_platform.material.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeachingMaterialRepository extends JpaRepository<TeachingMaterial, Long> {
    List<TeachingMaterial> findByTeacherProfile_IdOrderByCreatedAtDesc(Long teacherProfileId);
}
