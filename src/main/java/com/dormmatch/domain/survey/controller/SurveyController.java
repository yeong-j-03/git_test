package com.dormmatch.domain.survey.controller;

import com.dormmatch.domain.survey.dto.UserPreferencesRequestDto;
import com.dormmatch.domain.survey.dto.UserPreferencesResponseDto;
import com.dormmatch.domain.survey.service.SurveyService;
import com.dormmatch.global.aop.RequiresAuth;
import com.dormmatch.global.aop.RequiresSurvey;
import com.dormmatch.global.response.GlobalApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/surveys")
public class SurveyController {

    private SurveyService surveyService;

    @Autowired
    public SurveyController(SurveyService surveyService){
        this.surveyService = surveyService;
    }


    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "설문 제출 성공"),
            @ApiResponse(responseCode = "400", description = "필수 항목 누락 / 값 범위 초과"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음"),
            @ApiResponse(responseCode = "403", description = "기숙사 인증 미완료"),
            @ApiResponse(responseCode = "409", description = "이미 설문 제출 완료 상태")
    })
    @PostMapping("/api/surveys")
    @RequiresAuth
    public ResponseEntity<GlobalApiResponse<?>> saveUserPreferences(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserPreferencesRequestDto requestDto){

        surveyService.saveSurveyStatus(userId, requestDto);

        return ResponseEntity.status(200)
                .body(GlobalApiResponse.success(HttpStatus.OK,"설문이 제출되었습니다. 매칭 대기 상태로 전환됩니다.", null));
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설문 내용 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음"),
            @ApiResponse(responseCode = "403", description = "기숙사 인증 미완료"),
            @ApiResponse(responseCode = "404", description = "설문 제출 내역 없음"),
    })
    @GetMapping("/api/surveys/me")
    @RequiresAuth
    @RequiresSurvey
    public ResponseEntity<GlobalApiResponse<?>> getUserPreferences(
            @AuthenticationPrincipal Long userId
    ){
        UserPreferencesResponseDto responseDto = surveyService.getSurveyStatus(userId);

        return ResponseEntity.ok(
                GlobalApiResponse.success(HttpStatus.OK, "설문 내용 조회 성공", responseDto)
        );
    }

}
