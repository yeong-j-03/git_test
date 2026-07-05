package com.dormmatch.domain.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesResponseDto {

    private Long userId;
    private Boolean isCompleted;
    private Boolean isLocked;
    private Integer smokingStatus;
    private String introduce;
    private SurveyAnswers answers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
