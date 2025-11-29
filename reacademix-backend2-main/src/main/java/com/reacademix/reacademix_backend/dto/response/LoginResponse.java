package com.reacademix.reacademix_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 * JWT 토큰과 사용자 정보를 반환
 */
@Getter
@Builder
public class LoginResponse {

    private String token;
    private UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private Long userId;
        private String email;
        private String name;
        private String role;
    }
}

