package com.reacademix.reacademix_backend.controller;

import com.reacademix.reacademix_backend.dto.request.LoginRequest;
import com.reacademix.reacademix_backend.dto.response.LoginResponse;
import com.reacademix.reacademix_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 REST API Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 API
     * @param request 로그인 요청 DTO (email, password)
     * @return ResponseEntity<LoginResponse> 로그인 응답 (JWT 토큰, 사용자 정보)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

