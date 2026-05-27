package com.tsolmon.online_teaching_platform.auth.application;

import com.tsolmon.online_teaching_platform.auth.api.dto.AuthResponse;
import com.tsolmon.online_teaching_platform.auth.api.dto.LoginRequest;
import com.tsolmon.online_teaching_platform.auth.api.dto.RegisterRequest;
import com.tsolmon.online_teaching_platform.auth.api.dto.UpdateMyProfileRequest;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.cloudinary.Cloudinary;
import com.tsolmon.online_teaching_platform.auth.infrastructure.JwtProvider;
import com.tsolmon.online_teaching_platform.material.infrastructure.CloudinaryProperties;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
import com.tsolmon.online_teaching_platform.user.entity.User;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered", ex);
        }

        if (user.getRole() == Role.TEACHER) {
            TeacherProfile profile = new TeacherProfile();
            profile.setUser(user);
            teacherRepository.save(profile);
        }

        String token = jwtProvider.generateAccessToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtProvider.generateAccessToken(user);
        return new AuthResponse(token, UserResponse.from(user));
    }

    public UserResponse me(AuthUser authUser) {
        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(AuthUser authUser, UpdateMyProfileRequest request) {
        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        user.setFullName(request.fullName().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadAvatar(AuthUser authUser, MultipartFile file) {
        if (!cloudinaryProperties.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Image upload is not configured (set CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET)"
            );
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image too large (max 5MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required");
        }

        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        try {
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("folder", cloudinaryProperties.effectiveProfileFolder());
            uploadOptions.put("resource_type", "image");
            uploadOptions.put("overwrite", Boolean.FALSE);
            String preset = cloudinaryProperties.effectiveProfileUploadPreset();
            if (!preset.isBlank()) {
                uploadOptions.put("upload_preset", preset);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upload did not return a URL");
            }
            user.setAvatarUrl(secureUrl);
            User saved = userRepository.save(user);

            if (saved.getRole() == Role.TEACHER) {
                teacherRepository.findByUser_Id(saved.getId()).ifPresent(profile -> {
                    profile.setAvatarUrl(secureUrl);
                    teacherRepository.save(profile);
                });
            }

            return UserResponse.from(saved);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload image", e);
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
