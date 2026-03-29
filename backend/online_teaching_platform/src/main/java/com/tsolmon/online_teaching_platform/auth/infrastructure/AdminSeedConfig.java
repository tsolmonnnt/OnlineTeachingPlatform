package com.tsolmon.online_teaching_platform.auth.infrastructure;

import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.user.entity.User;
import com.tsolmon.online_teaching_platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminSeedConfig {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedAdminUser(
            @Value("${app.admin.email:admin@otp.mn}") String email,
            @Value("${app.admin.password:Admin123!}") String password,
            @Value("${app.admin.fullName:System Admin}") String fullName
    ) {
        return args -> {
            String normalizedEmail = email.trim().toLowerCase();
            if (userRepository.existsByEmail(normalizedEmail)) {
                return;
            }

            User admin = new User();
            admin.setFullName(fullName);
            admin.setEmail(normalizedEmail);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        };
    }
}

