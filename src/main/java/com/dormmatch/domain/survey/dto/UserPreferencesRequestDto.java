package com.dormmatch.domain.survey.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPreferencesRequestDto {

    @NotNull(message = "smokingStatus는 필수입니다.")
    @Min(value = 1, message = "smokingStatus는 1 이상이어야 합니다.")
    @Max(value = 2, message = "smokingStatus는 2 이하여야 합니다.")
    private Integer smokingStatus;

    @Size(max = 500, message = "introduce는 500자 이하여야 합니다.")
    private String introduce;

    @Valid
    @NotNull(message = "answers는 필수입니다.")
    private SurveyAnswers answers;
}
