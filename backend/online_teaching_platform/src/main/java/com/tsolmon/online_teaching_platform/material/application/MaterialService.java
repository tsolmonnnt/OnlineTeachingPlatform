package com.tsolmon.online_teaching_platform.material.application;

import com.cloudinary.Cloudinary;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.booking.application.CourseAccessService;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubject;
import com.tsolmon.online_teaching_platform.course.domain.CourseSubjectRepository;
import com.tsolmon.online_teaching_platform.material.api.dto.MaterialDownloadUrlResponse;
import com.tsolmon.online_teaching_platform.material.api.dto.TeachingMaterialResponse;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;
import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterialRepository;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryProperties;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryUrlService;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
    private final CloudinaryUrlService cloudinaryUrlService;

    @Transactional(readOnly = true)
    public List<TeachingMaterialResponse> listForTeacher(Long teacherProfileId, AuthUser viewer) {
        List<TeachingMaterial> list = materialRepository.findByTeacherProfile_IdOrderByCreatedAtDesc(teacherProfileId);
        boolean owner = isTeacherOwnerOfProfile(viewer, teacherProfileId);

        return list.stream()
                .map(m -> toResponse(m, includeSecureUrl(viewer, teacherProfileId, m, owner)))
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
            String resourceType = resolveUploadResourceType(file);
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("resource_type", resourceType);
            uploadOptions.put("type", "upload");
            uploadOptions.put("access_mode", "public");

            if (isPdfFile(file)) {
                // For raw PDFs: set public_id explicitly with .pdf extension
                // so Cloudinary stores and serves it with the correct extension
                String uniqueId = UUID.randomUUID().toString().replace("-", "");
                String publicId = cloudinaryProperties.effectiveFolder() + "/" + uniqueId + ".pdf";
                uploadOptions.put("public_id", publicId);
                // Do NOT set folder separately — it's already in public_id
            } else {
                uploadOptions.put("folder", cloudinaryProperties.effectiveFolder());
                uploadOptions.put("use_filename", Boolean.TRUE);
                uploadOptions.put("unique_filename", Boolean.TRUE);
            }

            String preset = cloudinaryProperties.uploadPreset();
            if (preset != null && !preset.isBlank()) {
                uploadOptions.put("upload_preset", preset.trim());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String uploadedResourceType = stringValue(uploadResult.get("resource_type"), resourceType);
            String deliveryType = stringValue(uploadResult.get("type"), "upload");
            Long version = longValue(uploadResult.get("version"));

            // Ensure secureUrl has .pdf extension for raw PDF files
            if (isPdfFile(file) && secureUrl != null && !secureUrl.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                secureUrl = secureUrl + ".pdf";
            }

            TeachingMaterial m = new TeachingMaterial();
            m.setTeacherProfile(teacher);
            m.setCourseSubject(courseSubject);
            m.setTitle(safeTitle);
            m.setDescription(description != null ? description.trim() : null);
            m.setCloudinaryPublicId(publicId);
            m.setCloudinaryResourceType(uploadedResourceType);
            m.setCloudinaryDeliveryType(deliveryType);
            m.setCloudinaryVersion(version);
            m.setSecureUrl(secureUrl);
            m.setContentType(file.getContentType());
            m.setSizeBytes(file.getSize());

            TeachingMaterial saved = materialRepository.save(m);
            return toResponse(saved, true);
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
                cloudinary.uploader().destroy(m.getCloudinaryPublicId(), cloudinaryUrlService.destroyOptions(m));
            } catch (IOException ignored) {
                // still remove DB row
            }
        }
        materialRepository.delete(m);
    }

    @Transactional(readOnly = true)
    public MaterialDownloadUrlResponse resolveDownloadUrl(Long materialId, AuthUser viewer) {
        String url = deliveryUrlForViewer(materialId, viewer);
        return new MaterialDownloadUrlResponse(url);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Void> downloadRedirect(Long materialId, AuthUser viewer) {
        String url = deliveryUrlForViewer(materialId, viewer);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    private String deliveryUrlForViewer(Long materialId, AuthUser viewer) {
        TeachingMaterial m = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found"));

        Long teacherProfileId = m.getTeacherProfile().getId();
        boolean owner = isTeacherOwnerOfProfile(viewer, teacherProfileId);
        if (!includeSecureUrl(viewer, teacherProfileId, m, owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this material");
        }

        String url = cloudinaryUrlService.deliveryUrl(m);
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material file is not available");
        }
        return url;
    }

    private TeachingMaterialResponse toResponse(TeachingMaterial m, boolean includeSecureUrl) {
        String url = includeSecureUrl ? cloudinaryUrlService.deliveryUrl(m) : null;
        return TeachingMaterialResponse.from(m, url);
    }

    private static String resolveUploadResourceType(MultipartFile file) {
        if (isPdfFile(file)) {
            return "raw";
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType != null && contentType.startsWith("video/")) {
            return "video";
        }
        if (contentType != null && !contentType.isBlank()) {
            return "raw";
        }
        return "image";
    }

    private static boolean isPdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && "application/pdf".equalsIgnoreCase(contentType)) {
            return true;
        }
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}