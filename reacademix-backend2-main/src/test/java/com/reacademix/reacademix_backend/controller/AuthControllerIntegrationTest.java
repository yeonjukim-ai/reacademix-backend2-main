package com.reacademix.reacademix_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reacademix.reacademix_backend.domain.user.User;
import com.reacademix.reacademix_backend.domain.user.UserRole;
import com.reacademix.reacademix_backend.domain.user.UserStatus;
import com.reacademix.reacademix_backend.dto.request.LoginRequest;
import com.reacademix.reacademix_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트
 * 실제 스프링 컨텍스트를 로드하여 전체 흐름을 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User testUser = User.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .name("테스트 사용자")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(testUser);
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("성공 - 올바른 자격 증명으로 로그인하면 200 OK와 JWT 토큰을 반환한다")
        void login_ValidCredentials_ReturnsOkWithToken() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.user.name").value("테스트 사용자"))
                    .andExpect(jsonPath("$.user.role").value("ADMIN"))
                    .andExpect(jsonPath("$.user.userId").isNumber());
        }

        @Test
        @DisplayName("성공 - 대소문자 구분 없이 이메일로 로그인할 수 있다")
        void login_CaseInsensitiveEmail_ReturnsOk() throws Exception {
            // given
            LoginRequest request = new LoginRequest("TEST@EXAMPLE.COM", TEST_PASSWORD);

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일로 로그인하면 401 Unauthorized를 반환한다")
        void login_NonExistentEmail_ReturnsUnauthorized() throws Exception {
            // given
            LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호로 로그인하면 401 Unauthorized를 반환한다")
        void login_WrongPassword_ReturnsUnauthorized() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 비활성화된 계정으로 로그인하면 401 Unauthorized를 반환한다")
        void login_InactiveAccount_ReturnsUnauthorized() throws Exception {
            // given - 별도 이메일로 비활성 사용자 생성
            String inactiveEmail = "inactive@example.com";
            User inactiveUser = User.builder()
                    .email(inactiveEmail)
                    .password(passwordEncoder.encode(TEST_PASSWORD))
                    .name("비활성 사용자")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.INACTIVE)
                    .build();
            userRepository.save(inactiveUser);

            LoginRequest request = new LoginRequest(inactiveEmail, TEST_PASSWORD);

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 이메일 형식이 올바르지 않으면 400 Bad Request를 반환한다")
        void login_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
            // given
            LoginRequest request = new LoginRequest("invalid-email", TEST_PASSWORD);

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 이메일이 비어있으면 400 Bad Request를 반환한다")
        void login_EmptyEmail_ReturnsBadRequest() throws Exception {
            // given
            LoginRequest request = new LoginRequest("", TEST_PASSWORD);

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 비어있으면 400 Bad Request를 반환한다")
        void login_EmptyPassword_ReturnsBadRequest() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "");

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
        void login_ShortPassword_ReturnsBadRequest() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, "short");

            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 요청 본문이 없으면 400 Bad Request를 반환한다")
        void login_NoRequestBody_ReturnsBadRequest() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON));

            // then
            result.andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("성공 - 응답 시간이 500ms 이내이다")
        void login_ResponseTime_Within500ms() throws Exception {
            // given
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            // when
            long startTime = System.currentTimeMillis();
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            long endTime = System.currentTimeMillis();

            // then
            long responseTime = endTime - startTime;
            org.assertj.core.api.Assertions.assertThat(responseTime).isLessThan(500);
        }
    }
}

