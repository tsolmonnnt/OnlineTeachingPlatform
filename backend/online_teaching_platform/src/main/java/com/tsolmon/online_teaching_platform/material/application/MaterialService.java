package com.tsolmon.online_teaching_platform.material.application;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.application.CourseAccessService;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.material.api.dto.TeachingMaterialResponse;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterialRepository;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryProperties;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaterialService {
    private static final long MAX_BYTES = 40 * 1024 * 1024;

    private final TeachingMaterialRepository materialRepository;
    private final TeacherRepository teacherRepository;
    private final CourseSubjectRepository courseSubjectRepository;
    private final CourseAccessService courseAccessService;
    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Transactional(readOnly = true)
    public List<TeachingMaterialResponse> listForTeacher(Long teacherProfileId, AuthUser viewer) {
        List<TeachingMaterial> list = materialRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(teacherProfileId);
        boolean owner = isTeacherOwnerOfProfile(viewer, teacherProfileId);

        return list.stream()
                .map(m -> TeachingMaterialResponse.from(m, includeSecureUrl(viewer, teacherProfileId, m, owner)))
                .toList();
    }

    private boolean isTeacherOwnerOfProfile(AuthUser viewer, Long teacherProfileId) {
        if (viewer == null || viewer.role() != Role.TEACHER) {
            return false;
        }
        return teacherRepository.findByUser_Id(viewer.id())
                .map(tp -> tp.getId().equals(teacherProfileId))
                .orElse(false);
    }

    private boolean includeSecureUrl(AuthUser viewer, Long teacherProfileId, TeachingMaterial m, boolean owner) {
        if (owner) {
            return true;
        }
        if (viewer == null || viewer.role() != Role.STUDENT) {
            return false;
        }
        if (m.getCourseSubject() == null) {
            return false;
        }
        return courseAccessService.hasConfirmedAccess(viewer.id(), teacherProfileId, m.getCourseSubject().getId());
    }

    @Transactional
    public TeachingMaterialResponse upload(
            AuthUser authUser,
            Long courseSubjectId,
            String title,
            String description,
            MultipartFile file
    ) {
        if (!cloudinaryProperties.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "File upload is not configured (set CLOUDINARY_* environment variables)"
            );
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large (max 40MB)");
        }
        if (courseSubjectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courseSubjectId is required");
        }

        TeacherProfile teacher = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));
        CourseSubject courseSubject = courseSubjectRepository.findById(courseSubjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown course subject"));
        if (!teacherOffersSubject(teacher, courseSubject)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add this subject to your profile before uploading materials for it");
        }

        String safeTitle = title == null || title.isBlank() ? file.getOriginalFilename() : title.trim();

        try {
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("folder", cloudinaryProperties.effectiveFolder());
            uploadOptions.put("resource_type", "auto");
            String preset = cloudinaryProperties.uploadPreset();
            if (preset != null && !preset.isBlank()) {
                uploadOptions.put("upload_preset", preset.trim());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");

            TeachingMaterial m = new TeachingMaterial();
            m.setTeacherProfile(teacher);
            m.setCourseSubject(courseSubject);
            m.setTitle(safeTitle);
            m.setDescription(description != null ? description.trim() : null);
            m.setCloudinaryPublicId(publicId);
            m.setSecureUrl(secureUrl);
            m.setContentType(file.getContentType());
            m.setSizeBytes(file.getSize());

            TeachingMaterial saved = materialRepository.save(m);
            return TeachingMaterialResponse.from(saved, true);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload file", e);
        }
    }

    private static boolean teacherOffersSubject(TeacherProfile teacher, CourseSubject subject) {
        return teacher.getSubjects().stream().anyMatch(s -> s.equalsIgnoreCase(subject.getName()));
    }

    @Transactional
    public void delete(AuthUser authUser, Long materialId) {
        TeachingMaterial m = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        TeacherProfile mine = teacherRepository.findByUser_Id(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher profile not found"));

        if (!m.getTeacherProfile().getId().equals(mine.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your material");
        }

        if (cloudinaryProperties.isConfigured()) {
            try {
                cloudinary.uploader().destroy(m.getCloudinaryPublicId(), ObjectUtils.emptyMap());
            } catch (IOException ignored) {
                // still remove DB row
            }
        }
        materialRepository.delete(m);
    }
}
