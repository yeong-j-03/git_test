package com.dormmatch.domain.auth.controller;

import com.dormmatch.domain.auth.dto.AuthStatusResponseDto;
import com.dormmatch.domain.auth.dto.LoginResponseDto;
import com.dormmatch.domain.auth.dto.RefreshTokenResponseDto;
import com.dormmatch.domain.auth.service.AuthService;
import com.dormmatch.global.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AuthStatusResponseDto>> status() {
        AuthStatusResponseDto response = authService.getCurrentUserStatus();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", clearCookie.toString());
        return ResponseEntity.ok(ApiResponse.successMessage("로그아웃되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refresh(
            @RequestParam(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractRefreshToken(request);
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Refresh Token이 필요합니다."));
        }

        RefreshTokenResponseDto response = authService.refreshAccessToken(refreshToken);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었거나 유효하지 않습니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoCallback(
            @RequestParam(value = "code", required = false) String code,
            HttpServletResponse servletResponse
    ) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "인가 코드가 필요합니다."));
        }

        AuthService.LoginResult loginResult = authService.loginOrRegister(code);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResult.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .sameSite("Lax")
                .build();

        servletResponse.addHeader("Set-Cookie", refreshTokenCookie.toString());
        return ResponseEntity.ok(ApiResponse.success(loginResult.response()));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
