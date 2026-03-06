package com.tsolmon.online_teaching_platform.auth.infrastructure;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.accessTokenTtlMinutes}") long accessTokenTtlMinutes
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenTtl);

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        "userId", user.getId(),
                        "role", user.getRole().name()
                ))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public AuthUser parse(String token) throws JwtException {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();
        Number userIdNumber = claims.get("userId", Number.class);
        Long userId = userIdNumber == null ? null : userIdNumber.longValue();
        String roleString = claims.get("role", String.class);
        Role role = Role.valueOf(roleString);
        String email = claims.getSubject();

        return new AuthUser(userId, email, role);
    }
}
