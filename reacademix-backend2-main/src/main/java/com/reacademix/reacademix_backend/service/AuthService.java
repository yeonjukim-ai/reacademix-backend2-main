package com.reacademix.reacademix_backend.service;

import com.reacademix.reacademix_backend.domain.user.User;
import com.reacademix.reacademix_backend.domain.user.UserStatus;
import com.reacademix.reacademix_backend.dto.request.LoginRequest;
import com.reacademix.reacademix_backend.dto.response.LoginResponse;
import com.reacademix.reacademix_backend.exception.AuthenticationException;
import com.reacademix.reacademix_backend.repository.UserRepository;
import com.reacademix.reacademix_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직을 처리하는 Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인 처리
     * @param request 로그인 요청 DTO
     * @return LoginResponse 로그인 응답 DTO (JWT 토큰 포함)
     * @throws AuthenticationException 인증 실패 시 (이메일 없음, 비밀번호 불일치, 비활성 계정)
     */
    public LoginResponse login(LoginRequest request) {
        // 이메일로 사용자 조회 (소문자로 정규화)
        String normalizedEmail = request.getEmail().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 계정 상태 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("비활성화된 계정입니다.");
        }

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // 응답 생성
        return buildLoginResponse(token, user);
    }

    /**
     * LoginResponse 생성
     * @param token JWT 토큰
     * @param user User 엔티티
     * @return LoginResponse
     */
    private LoginResponse buildLoginResponse(String token, User user) {
        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}

