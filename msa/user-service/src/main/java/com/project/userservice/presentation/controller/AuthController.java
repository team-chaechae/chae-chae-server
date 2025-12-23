package com.project.userservice.presentation.controller;

import com.project.userservice.application.global.dto.ResDTO;
import com.project.userservice.application.response.ResAuthLoginDTO;
import com.project.userservice.application.service.AuthService;
import com.project.userservice.presentation.request.ReqAuthPostLoginDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 - 사용자 인증 후 JWT 토큰 발급
     */
    @PostMapping("/login")
    public ResponseEntity<ResDTO<ResAuthLoginDTO>> login(
            @Valid @RequestBody ReqAuthPostLoginDTO request
    ) {
        ResAuthLoginDTO loginResponse = authService.login(request);

        return ResponseEntity.ok(
                ResDTO.<ResAuthLoginDTO>builder()
                        .code(HttpStatus.OK.value())
                        .message("로그인 성공")
                        .data(loginResponse)
                        .build()
        );
    }

    /**
     * 로그아웃 - 토큰 블랙리스트 추가
     */
    @PostMapping("/logout")
    public ResponseEntity<ResDTO<Void>> logout(
            @RequestHeader("Authorization") String token
    ) {
        authService.logout(token);

        return ResponseEntity.ok(
                ResDTO.<Void>builder()
                        .code(HttpStatus.OK.value())
                        .message("로그아웃 성공")
                        .build()
        );
    }

    /**
     * 토큰 갱신 - Refresh Token으로 새 Access Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResDTO<ResAuthLoginDTO>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken
    ) {
        ResAuthLoginDTO refreshResponse = authService.refresh(refreshToken);

        return ResponseEntity.ok(
                ResDTO.<ResAuthLoginDTO>builder()
                        .code(HttpStatus.OK.value())
                        .message("토큰 갱신 성공")
                        .data(refreshResponse)
                        .build()
        );
    }
}
