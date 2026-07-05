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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getProfile(
            @AuthenticationPrincipal Long userId
    ) {
        UserProfileResponseDto response = userDetailsService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileUpdateResponseDto>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequestDto request
    ) {
        UserProfileUpdateResponseDto response = userDetailsService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.successMessage("프로필이 수정되었습니다.", response));
    }

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<UserDetailsResponseDto>> createDetails(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDetailsRequestDto request
    ) {
        UserDetailsResponseDto response = userDetailsService.createDetails(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "필수 정보가 등록되었습니다.", response));
    }
}
