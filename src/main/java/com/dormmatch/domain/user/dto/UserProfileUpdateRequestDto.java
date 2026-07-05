package com.dormmatch.domain.user.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequestDto {

    private String nickname;
    private String profileImageUrl;

    @AssertTrue(message = "nickname은 공백일 수 없습니다.")
    public boolean isNicknameValid() {
        return nickname == null || !nickname.isBlank();
    }

    @AssertTrue(message = "profileImageUrl은 공백일 수 없습니다.")
    public boolean isProfileImageUrlValid() {
        return profileImageUrl == null || !profileImageUrl.isBlank();
    }
}
