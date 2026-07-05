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

    /**
     * [GET] /api/v1/auth/kakao/callback
     * 역할: 카카오가 로그인을 마친 사용자를 이 주소로 보내면서 일회용 티켓('code')을 던져줍니다.
     * 그 티켓을 받아 실제 회원가입/로그인 처리를 완료하고 토큰을 발급하는 핵심 API입니다.
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<ApiResponse<LoginResponseDto>> kakaoCallback(
            @RequestParam(value = "code", required = false) String code, // 카카오가 보내준 인가 코드
            HttpServletResponse servletResponse
    ) {
        // 만약 카카오가 코드를 안 보냈거나 비어있다면 잘못된 요청시 에러를 반환합니다.
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST, "인가 코드가 필요합니다."));
        }

        // 1. [비즈니스 로직 호출]: 서비스에게 일회용 코드를 주면서 카카오와 통신하여 회원가입/로그인을 시키고
        //    그 결과물(Access/Refresh Token 및 유저정보)을 받아옵니다.
        AuthService.LoginResult loginResult = authService.loginOrRegister(code);

        // 2. [보안 조치]: 탈취 위험이 높은 Refresh Token은 브라우저의 로컬 스토리지에 저장하면 보안에 취약하므로,
        //    자바스크립트가 접근할 수 없는 안전한 'HttpOnly 쿠키' 형태로 만듭니다.
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", loginResult.refreshToken())
                .httpOnly(true)          // 브라우저에서 스크립트로 쿠키 탈취 불가능하게 설정 (XSS 공격 방지)
                .secure(false)           // 로컬 개발 환경(http)에서도 테스트 가능하도록 우선 false 처리 (실배포시 true 권장)
                .path("/")               // 우리 서버의 모든 주소 경로에서 이 쿠키를 사용할 수 있게 세팅
                .maxAge(Duration.ofDays(14)) // 쿠키의 유효기간을 14일(2주일)로 설정
                .sameSite("Lax")         // 크로스 사이트 요청 위조(CSRF) 공격을 방지하는 브라우저 보안 정책
                .build();

        // 3. [헤더 설정]: 만든 HttpOnly 쿠키를 브라우저의 지갑에 쏙 넣어주도록 응답 헤더(Set-Cookie)에 추가합니다.
        servletResponse.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 4. [최종 반환]: 프론트엔드에게 Access Token과 유저 간략 정보가 담긴 데이터를 규격 상자에 담아 반환합니다.
        return ResponseEntity.ok(ApiResponse.success(loginResult.response()));
    }

    /**
     * [POST] /api/v1/auth/refresh
     * 역할: 로그인의 유효기간(Access Token 만료)이 다 되었을 때, 브라우저 쿠키에 숨겨둔
     * Refresh Token을 꺼내어 검증한 뒤 새로운 Access Token을 갱신(재발급)해 줍니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refresh(
            @RequestParam(value = "refreshToken", required = false) String refreshToken, // URL 파라미터로 들어올 경우를 대비
            HttpServletRequest request
    ) {
        // 만약 주소창으로 토큰이 안 들어왔다면, 요청 헤더의 쿠키 보관함에서 직접 리프레시 토큰을 꺼내는 함수를 호출합니다.
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractRefreshToken(request);
        }
        // 쿠키에도 리프레시 토큰이 전혀 없다면 인증 실패(41) 에러를 뱉습니다.
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Refresh Token이 필요합니다."));
        }

        // 서비스에게 리프레시 토큰을 주며 새 엑세스 토큰을 받아옵니다.
        RefreshTokenResponseDto response = authService.refreshAccessToken(refreshToken);

        // 만약 리프레시 토큰마저 만료되었거나 썩은 토큰이라면 다시 로그인하라고 에러(401)를 던집니다.
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었거나 유효하지 않습니다."));
        }

        // 정상적으로 새 Access Token이 나왔다면 프론트엔드에게 돌려줌
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [POST] /api/v1/auth/logout
     * 역할: 로그아웃 요청 시, 브라우저에 저장되어 있던 HttpOnly 쿠키(Refresh Token)를 강제로 만료시켜 무효화합니다.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        // 똑같은 이름의 쿠키를 만들되, 만료시간(maxAge)을 0으로 주어 브라우저가 즉시 삭제하게 만듭니다.
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // 즉시 만료
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", clearCookie.toString());
        return ResponseEntity.ok(ApiResponse.successMessage("로그아웃되었습니다."));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AuthStatusResponseDto>> status() {
        AuthStatusResponseDto response = authService.getCurrentUserStatus();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //사용자의 HTTP 요청 속 refresh토큰만 추출
    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue(); // 찾았다면 토큰 문자열 반환
            }
        }

        return null;
    }
}