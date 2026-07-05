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

    // 외부 API(카카오 서버)와 HTTP 통신을 주고받기 위함
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public LoginResult loginOrRegister(String code) {
        //카카오 인증 서버에 인가 코드를 보내어 '카카오 전용 엑세스 토큰'을 받아옵니다.
        KakaoTokenResponseDto tokenResponse = getKakaoToken(code);

        //받은 카카오 토큰을 들고 카카오 자원 서버에 가서 '사용자 프로필 정보'를 받아옴
        KakaoUserInfoResponseDto userInfo = getKakaoUserInfo(tokenResponse.getAccessToken());

        //카카오 회원번호를 바탕으로 우리 DB에 있는 유저인지 찾고, 없으면 새로 가입
        UserRegistration userRegistration = findOrCreateUser(userInfo);
        Users user = userRegistration.user();

        //JWT Access Token과 Refresh Token 발행
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole());

        // 결과반환
        LoginResponseDto response = LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(userRegistration.isNewUser()) // 최초 가입자인지 기존 회원인지 여부
                .user(LoginResponseDto.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .build())
                .build();

        return new LoginResult(response, refreshToken);
    }

    /**
     * 핵심 메서드 2: 기존 Refresh Token 검증 후 새로운 Access Token을 생성해줍니다.
     */
    public RefreshTokenResponseDto refreshAccessToken(String refreshToken) {
        // 리프레시 토큰 검증
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return null; // 가짜거나 만료되었다면 탈락
        }

        // 토큰 내용물을 열어서 유저 ID를 꺼냄
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());

        // 그 유저가 실제로 우리 DB에 여전히 존재하는 유저인지 확인
        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        // Access Token만 새로 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());

        return RefreshTokenResponseDto.builder()
                .accessToken(accessToken)
                .build();
    }

    public AuthStatusResponseDto getCurrentUserStatus() {
        // 스프링 시큐리티에서 현재 요청을 보낸 인증 객체를 꺼냄
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 객체가 비어있거나, 로그인 안 한 익명 사용자("anonymousUser")라면 인증 실패(authenticated=false) 구조를 반환합니다.
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return AuthStatusResponseDto.builder().authenticated(false).build();
        }

        Long userId;
        try {
            userId = Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return AuthStatusResponseDto.builder().authenticated(false).build();
        }

        // DB에서 유저를 조회하여 닉네임, 역할(Role), 설문 작성 여부(`isSurveyCompleted`) 등을 묶어서 알려줍니다.
        Users user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return AuthStatusResponseDto.builder().authenticated(false).build();
        }

        return AuthStatusResponseDto.builder()
                .authenticated(true)
                .user(AuthStatusResponseDto.UserInfo.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .certificationStatus(null)
                        .surveyCompleted(isSurveyCompleted(user.getId())) // 설문 완료 상태 동적 확인
                        .build())
                .build();
    }

    //DB를 조회하여 가입된 유저면 그대로 가져오고, 없으면 카카오 정보로 회원가입(Save) 시킵니다.
    private UserRegistration findOrCreateUser(KakaoUserInfoResponseDto userInfo) {
        String oauthId = userInfo.getId().toString(); // 카카오의 고유 회원번호

        Optional<Users> existingUser = usersRepository.findByOauthId(oauthId);
        if (existingUser.isPresent()) {
            return new UserRegistration(existingUser.get(), false); // 기존 유저 (isNewUser = false)
        }

        // 처음 가입하는 유저라면 새롭게 유저 엔티티를 조립하여 DB에 저장합니다.
        KakaoUserInfoResponseDto.KakaoAccount.Profile profile = userInfo.getKakaoAccount().getProfile();
        Users newUser = Users.builder()
                .oauthId(oauthId)
                .email(userInfo.getKakaoAccount().getEmail())
                .nickname(profile.getNickname())
                .profileImageUrl(profile.getProfileImageUrl())
                .build();
        return new UserRegistration(usersRepository.save(newUser), true); // 신규 유저 (isNewUser = true)
    }

    //RestTemplate을 활용해 진짜 카카오 서버에 HTTP POST 요청을 날려 토큰을 받아옵니다.
    private KakaoTokenResponseDto getKakaoToken(String code) {
        String tokenUri = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // 카카오가 요구하는 헤더 타입

        // application.properties와 .env에서 파싱해온 카카오 정보들을 파라미터로 조립합니다.
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoProperties.getClientId());
        params.add("client_secret", kakaoProperties.getClientSecret());
        params.add("redirect_uri", kakaoProperties.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // 카카오에 전송하여 응답을 (KakaoTokenResponseDto)에 바로 매핑합니다.
            return restTemplate.postForObject(tokenUri, request, KakaoTokenResponseDto.class);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.KAKAO_API_ERROR); // 통신 에러 시 예외 처리
        }
    }

    //방금 받은 카카오 엑세스 토큰을 헤더에 실어 카카오 유저 정보 API를 호출합니다.
    private KakaoUserInfoResponseDto getKakaoUserInfo(String accessToken) {
        String userInfoUri = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // 카카오 토큰을 Bearer 헤더에 주입

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(userInfoUri, HttpMethod.GET, request, KakaoUserInfoResponseDto.class).getBody();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
        }
    }

    private boolean isSurveyCompleted(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
                .map(userPreferences -> Boolean.TRUE.equals(userPreferences.getIsCompleted()))
                .orElse(false);
    }

    // 서비스 안에서만 임시로 2개 이상의 결과값을 묶어서 리턴하기 위해 선언
    public record LoginResult(LoginResponseDto response, String refreshToken) {}
    private record UserRegistration(Users user, boolean isNewUser) {}
}