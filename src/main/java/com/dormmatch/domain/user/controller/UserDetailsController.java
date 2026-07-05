package com.dormmatch.domain.user.controller;

import com.dormmatch.domain.user.dto.UserDetailsRequestDto;
import com.dormmatch.domain.user.dto.UserDetailsResponseDto;
import com.dormmatch.domain.user.dto.UserProfileResponseDto;
import com.dormmatch.domain.user.dto.UserProfileUpdateRequestDto;
import com.dormmatch.domain.user.dto.UserProfileUpdateResponseDto;
import com.dormmatch.domain.user.service.UserDetailsService;
import com.dormmatch.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserDetailsController {

    private final UserDetailsService userDetailsService;

    /**

     * @AuthenticationPrincipal:시큐리티 필터(JwtAuthenticationFilter)가 토큰을 검증한 뒤
     * 저장해둔 유저의 PK 값(userId)을 안전하게 꺼내와 매개변수에 주입해줍니다.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getProfile(
            @AuthenticationPrincipal Long userId
    ) {
        // 1. 서비스에 유저 ID를 넘겨 프로필 데이터를 조회합니다.
        UserProfileResponseDto response = userDetailsService.getProfile(userId);

        // 2. 성공 응답 포맷(ApiResponse.success)에 담아 HTTP 상태코드 200(OK)으로 반환합니다.
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [PATCH] /api/v1/users/me
     * 역할: 현재 로그인한 유저의 프로필(닉네임, 프로필 이미지)을 일부 수정합니다
     * @RequestBody: 클라이언트가 보낸 JSON 데이터를 자바 객체(Dto)로 변환해줍니다
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileUpdateResponseDto>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequestDto request
    ) {
        // 1. 서비스에 유저 ID와 수정할 내용(request)을 넘겨 데이터를 업데이트합니다.
        UserProfileUpdateResponseDto response = userDetailsService.updateProfile(userId, request);

        // 2. 수정 완료 메시지와 함께 가공된 결과 데이터를 반환합니다.
        return ResponseEntity.ok(ApiResponse.successMessage("프로필이 수정되었습니다.", response));
    }

    /**
     * [POST] /api/v1/users/details
     * 역할: 카카오 로그인 직후, 서비스 이용에 필요한 추가 필수 정보(본명, 학번, 나이, 성별, 학과)를 최초 등록합니다.
     */
    @PostMapping("/details")
    public ResponseEntity<ApiResponse<UserDetailsResponseDto>> createDetails(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDetailsRequestDto request
    ) {
        // 1. 서비스에 유저 ID와 필수 정보 입력 데이터를 넘겨 새로운 정보(UserDetails)를 생성합니다.
        UserDetailsResponseDto response = userDetailsService.createDetails(userId, request);

        // 2. 새로운 데이터가 생성되었으므로 HTTP 상태코드 201(CREATED)을 지정하여 반환합니다.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "필수 정보가 등록되었습니다.", response));
    }
}