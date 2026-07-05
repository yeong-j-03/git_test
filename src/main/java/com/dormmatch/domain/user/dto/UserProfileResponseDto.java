package com.dormmatch.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponseDto {

    private final Long id;
    private final String nickname;
    private final String email;
    private final String profileImageUrl;
    private final String role;
    private final String status;
    private final Detail detail;

    @Getter
    @Builder
    public static class Detail {
        private final String realName;
        private final String studentId;
        private final Integer age;
        private final String gender;
        private final String department;
    }
}
