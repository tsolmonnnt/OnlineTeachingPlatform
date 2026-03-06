package com.tsolmon.online_teaching_platform.auth.application;

import com.tsolmon.online_teaching_platform.auth.api.dto.AuthResponse;
import com.tsolmon.online_teaching_platform.auth.api.dto.LoginRequest;
import com.tsolmon.online_teaching_platform.auth.api.dto.RegisterRequest;
import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.auth.infrastructure.JwtProvider;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherProfile;
import com.tsolmon.online_teaching_platform.teacher.domain.TeacherRepository;
import com.tsolmon.online_teaching_platform.user.api.dto.UserResponse;
import com.tsolmon.online_teaching_platform.user.entity.User;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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
        userRepository.save(user);

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

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
