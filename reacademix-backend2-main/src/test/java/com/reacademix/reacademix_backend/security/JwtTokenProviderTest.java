package com.reacademix.reacademix_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 단위 테스트
 */
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = "test-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256";
    private static final long EXPIRATION = 86400000L; // 24시간

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("createToken() 메서드")
    class CreateTokenMethod {

        @Test
        @DisplayName("성공 - 유효한 JWT 토큰을 생성한다")
        void createToken_Success() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String role = "ADMIN";

            // when
            String token = jwtTokenProvider.createToken(userId, email, role);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT는 3부분으로 구성
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken() 메서드")
    class GetUserIdFromTokenMethod {

        @Test
        @DisplayName("성공 - 토큰에서 사용자 ID를 추출한다")
        void getUserIdFromToken_Success() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String role = "ADMIN";
            String token = jwtTokenProvider.createToken(userId, email, role);

            // when
            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedUserId).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("getEmailFromToken() 메서드")
    class GetEmailFromTokenMethod {

        @Test
        @DisplayName("성공 - 토큰에서 이메일을 추출한다")
        void getEmailFromToken_Success() {
            // given
            Long userId = 1L;
            String email = "test@example.com";
            String role = "ADMIN";
            String token = jwtTokenProvider.createToken(userId, email, role);

            // when
            String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

            // then
            assertThat(extractedEmail).isEqualTo(email);
        }
    }

    @Nested
    @DisplayName("validateToken() 메서드")
    class ValidateTokenMethod {

        @Test
        @DisplayName("성공 - 유효한 토큰은 true를 반환한다")
        void validateToken_ValidToken_ReturnsTrue() {
            // given
            String token = jwtTokenProvider.createToken(1L, "test@example.com", "ADMIN");

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("실패 - 잘못된 형식의 토큰은 false를 반환한다")
        void validateToken_InvalidToken_ReturnsFalse() {
            // given
            String invalidToken = "invalid.jwt.token";

            // when
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("실패 - 빈 토큰은 false를 반환한다")
        void validateToken_EmptyToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("실패 - null 토큰은 false를 반환한다")
        void validateToken_NullToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("실패 - 만료된 토큰은 false를 반환한다")
        void validateToken_ExpiredToken_ReturnsFalse() {
            // given - 만료 시간이 0ms인 토큰 생성기
            JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(SECRET, 0L);
            String expiredToken = expiredTokenProvider.createToken(1L, "test@example.com", "ADMIN");

            // when
            boolean isValid = jwtTokenProvider.validateToken(expiredToken);

            // then
            assertThat(isValid).isFalse();
        }
    }
}

