package com.dormmatch.domain.auth.service;

import com.dormmatch.domain.auth.dto.AuthStatusResponseDto;
import com.dormmatch.domain.auth.dto.KakaoTokenResponseDto;
import com.dormmatch.domain.auth.dto.KakaoUserInfoResponseDto;
import com.dormmatch.domain.auth.dto.LoginResponseDto;
import com.dormmatch.domain.auth.dto.RefreshTokenResponseDto;
import com.dormmatch.domain.survey.repository.UserPreferencesRepository;
import com.dormmatch.domain.user.entity.Users;
import com.dormmatch.domain.user.repository.UsersRepository;
import com.dormmatch.global.config.KakaoProperties;
import com.dormmatch.global.exception.BusinessException;
import com.dormmatch.global.exception.ErrorCode;
import com.dormmatch.global.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoProperties kakaoProperties;
    private final UsersRepository usersRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public LoginResult loginOrRegister(String code) {
        KakaoTokenResponseDto tokenResponse = getKakaoToken(code);
        KakaoUserInfoResponseDto userInfo = getKakaoUserInfo(tokenResponse.getAccessToken());

        UserRegistration userRegistration = findOrCreateUser(userInfo);
        Users user = userRegistration.user();

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole());

        LoginResponseDto response = LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(userRegistration.isNewUser())
                .user(LoginResponseDto.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .build())
                .build();

        return new LoginResult(response, refreshToken);
    }

    public RefreshTokenResponseDto refreshAccessToken(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return null;
        }

        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());

        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());

        return RefreshTokenResponseDto.builder()
                .accessToken(accessToken)
                .build();
    }

    public AuthStatusResponseDto getCurrentUserStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return AuthStatusResponseDto.builder()
                    .authenticated(false)
                    .build();
        }

        Long userId;
        try {
            userId = Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return AuthStatusResponseDto.builder()
                    .authenticated(false)
                    .build();
        }

        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return AuthStatusResponseDto.builder()
                    .authenticated(false)
                    .build();
        }

        return AuthStatusResponseDto.builder()
                .authenticated(true)
                .user(AuthStatusResponseDto.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .certificationStatus(null)
                        .surveyCompleted(isSurveyCompleted(user.getId()))
                        .build())
                .build();
    }

    private boolean isSurveyCompleted(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
                .map(userPreferences -> Boolean.TRUE.equals(userPreferences.getIsCompleted()))
                .orElse(false);
    }

    public record LoginResult(
            LoginResponseDto response,
            String refreshToken
    ) {
    }

    private record UserRegistration(
            Users user,
            boolean isNewUser
    ) {
    }

    private UserRegistration findOrCreateUser(KakaoUserInfoResponseDto userInfo) {
        String oauthId = userInfo.getId().toString();

        Optional<Users> existingUser = usersRepository.findByOauthId(oauthId);
        if (existingUser.isPresent()) {
            return new UserRegistration(existingUser.get(), false);
        }

        KakaoUserInfoResponseDto.KakaoAccount.Profile profile = userInfo.getKakaoAccount().getProfile();
        Users newUser = Users.builder()
                .oauthId(oauthId)
                .email(userInfo.getKakaoAccount().getEmail())
                .nickname(profile.getNickname())
                .profileImageUrl(profile.getProfileImageUrl())
                .build();
        return new UserRegistration(usersRepository.save(newUser), true);
    }

    private KakaoTokenResponseDto getKakaoToken(String code) {
        String tokenUri = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoProperties.getClientId());
        params.add("client_secret", kakaoProperties.getClientSecret());
        params.add("redirect_uri", kakaoProperties.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            KakaoTokenResponseDto response = restTemplate.postForObject(tokenUri, request, KakaoTokenResponseDto.class);
            if (response == null || response.getAccessToken() == null) {
                throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
            }
            return response;
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
        }
    }

    private KakaoUserInfoResponseDto getKakaoUserInfo(String accessToken) {
        String userInfoUri = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            KakaoUserInfoResponseDto response = restTemplate.exchange(userInfoUri, HttpMethod.GET, request, KakaoUserInfoResponseDto.class).getBody();
            if (response == null || response.getId() == null || response.getKakaoAccount() == null
                    || response.getKakaoAccount().getProfile() == null) {
                throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
            }
            return response;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
        }
    }
}
