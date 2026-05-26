package com.tsolmon.online_teaching_platform.auth.infrastructure;

import com.tsolmon.online_teaching_platform.auth.domain.AuthUser;
import com.tsolmon.online_teaching_platform.auth.domain.Role;
import com.tsolmon.online_teaching_platform.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    @Test
    void constructorRejectsMissingOrShortSecret() {
        assertThatThrownBy(() -> new JwtProvider(null, 15))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProvider("short", 15))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generatedTokenParsesBackToAuthUser() {
        JwtProvider provider = new JwtProvider("0123456789abcdef0123456789abcdef", 15);
        User user = new User();
        user.setId(42L);
        user.setEmail("student@test.mn");
        user.setRole(Role.STUDENT);

        AuthUser parsed = provider.parse(provider.generateAccessToken(user));

        assertThat(parsed.id()).isEqualTo(42L);
        assertThat(parsed.email()).isEqualTo("student@test.mn");
        assertThat(parsed.role()).isEqualTo(Role.STUDENT);
    }
}
