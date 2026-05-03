package com.tsolmon.online_teaching_platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/stats").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/dashboard/teacher").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/student").hasRole("STUDENT")

                        .requestMatchers(HttpMethod.POST, "/api/teachers/me/avatar").hasRole("TEACHER")
                        .requestMatchers("/api/teachers/me").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/teachers", "/api/teachers/*").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/course/subjects/teaching").hasRole("TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/course/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/schedules/teacher/*").permitAll()
                        // "/api/schedules/me/**" does not match the exact path "/api/schedules/me" (no extra segment)
                        .requestMatchers(HttpMethod.GET, "/api/schedules/me").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/schedules/me").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/schedules/me/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/schedules/me/*").hasRole("TEACHER")

                        .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/confirm").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel").authenticated()

                        .requestMatchers("/api/notifications/**").authenticated()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reviews/teacher/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("STUDENT")

                        .requestMatchers(HttpMethod.GET, "/api/materials/teacher/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/materials").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/materials/**").hasRole("TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/quizzes/teacher/*/published").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/*/public").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/quizzes/*/attempts").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/quizzes").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/quizzes/mine/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/mine/**").hasRole("TEACHER")

                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Username/password auth disabled (use JWT)");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:5174",
                "http://127.0.0.1:5174"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
