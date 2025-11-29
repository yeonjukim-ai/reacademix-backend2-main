package com.reacademix.reacademix_backend.service;

import com.reacademix.reacademix_backend.domain.user.User;
import com.reacademix.reacademix_backend.domain.user.UserRole;
import com.reacademix.reacademix_backend.domain.user.UserStatus;
import com.reacademix.reacademix_backend.dto.request.LoginRequest;
import com.reacademix.reacademix_backend.dto.response.LoginResponse;
import com.reacademix.reacademix_backend.exception.AuthenticationException;
import com.reacademix.reacademix_backend.repository.UserRepository;
import com.reacademix.reacademix_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String TEST_TOKEN = "test.jwt.token";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .name("테스트 사용자")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
    }

    @Nested
    @DisplayName("login() 메서드")
    class LoginMethod {

        @Test
        @DisplayName("성공 - 올바른 이메일과 비밀번호로 로그인하면 JWT 토큰을 반환한다")
        void login_Success_ReturnsJwtToken() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtTokenProvider.createToken(any(), anyString(), anyString())).willReturn(TEST_TOKEN);

            // when
            LoginResponse response = authService.login(loginRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(TEST_TOKEN);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getUser().getName()).isEqualTo("테스트 사용자");
            assertThat(response.getUser().getRole()).isEqualTo("ADMIN");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
            verify(jwtTokenProvider).createToken(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("성공 - 대소문자 구분 없이 이메일로 로그인할 수 있다")
        void login_CaseInsensitiveEmail_Success() {
            // given
            LoginRequest upperCaseRequest = new LoginRequest("TEST@EXAMPLE.COM", TEST_PASSWORD);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtTokenProvider.createToken(any(), anyString(), anyString())).willReturn(TEST_TOKEN);

            // when
            LoginResponse response = authService.login(upperCaseRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(TEST_TOKEN);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일로 로그인 시 AuthenticationException 발생")
        void login_NonExistentEmail_ThrowsException() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtTokenProvider, never()).createToken(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호로 로그인 시 AuthenticationException 발생")
        void login_WrongPassword_ThrowsException() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
            verify(jwtTokenProvider, never()).createToken(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 비활성화된 계정으로 로그인 시 AuthenticationException 발생")
        void login_InactiveAccount_ThrowsException() {
            // given
            User inactiveUser = User.builder()
                    .email(TEST_EMAIL)
                    .password(ENCODED_PASSWORD)
                    .name("비활성 사용자")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.INACTIVE)
                    .build();

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(inactiveUser));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("비활성화된 계정입니다.");

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
            verify(jwtTokenProvider, never()).createToken(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 정지된 계정으로 로그인 시 AuthenticationException 발생")
        void login_SuspendedAccount_ThrowsException() {
            // given
            User suspendedUser = User.builder()
                    .email(TEST_EMAIL)
                    .password(ENCODED_PASSWORD)
                    .name("정지된 사용자")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.SUSPENDED)
                    .build();

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(suspendedUser));
            given(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("비활성화된 계정입니다.");
        }
    }
}

