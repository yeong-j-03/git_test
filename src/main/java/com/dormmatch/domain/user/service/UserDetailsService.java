package com.dormmatch.domain.user.service;

import com.dormmatch.domain.user.dto.UserDetailsRequestDto;
import com.dormmatch.domain.user.dto.UserDetailsResponseDto;
import com.dormmatch.domain.user.dto.UserProfileResponseDto;
import com.dormmatch.domain.user.dto.UserProfileUpdateRequestDto;
import com.dormmatch.domain.user.dto.UserProfileUpdateResponseDto;
import com.dormmatch.domain.user.entity.UserDetails;
import com.dormmatch.domain.user.entity.Users;
import com.dormmatch.domain.user.repository.UserDetailsRepository;
import com.dormmatch.domain.user.repository.UsersRepository;
import com.dormmatch.global.exception.BusinessException;
import com.dormmatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailsService {

    private final UsersRepository usersRepository;
    private final UserDetailsRepository userDetailsRepository;

    @Transactional
    public UserDetailsResponseDto createDetails(Long userId, UserDetailsRequestDto request) {
        if (userDetailsRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.DETAILS_ALREADY_EXISTS);
        }

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserDetails userDetails = UserDetails.builder()
                .user(user)
                .realName(request.getRealName())
                .studentId(request.getStudentId())
                .age(request.getAge())
                .gender(request.getGender())
                .department(request.getDepartment())
                .build();

        UserDetails savedDetails = userDetailsRepository.save(userDetails);
        return toResponse(savedDetails);
    }

    public UserProfileResponseDto getProfile(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserDetails userDetails = userDetailsRepository.findById(userId).orElse(null);

        return UserProfileResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .detail(toProfileDetail(userDetails))
                .build();
    }

    @Transactional
    public UserProfileUpdateResponseDto updateProfile(Long userId, UserProfileUpdateRequestDto request) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(request.getNickname(), request.getProfileImageUrl());

        return UserProfileUpdateResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    private UserDetailsResponseDto toResponse(UserDetails userDetails) {
        return UserDetailsResponseDto.builder()
                .userId(userDetails.getUserId())
                .realName(userDetails.getRealName())
                .studentId(userDetails.getStudentId())
                .age(userDetails.getAge())
                .gender(userDetails.getGender())
                .department(userDetails.getDepartment())
                .build();
    }

    private UserProfileResponseDto.Detail toProfileDetail(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        return UserProfileResponseDto.Detail.builder()
                .realName(userDetails.getRealName())
                .studentId(userDetails.getStudentId())
                .age(userDetails.getAge())
                .gender(userDetails.getGender())
                .department(userDetails.getDepartment())
                .build();
    }
}
