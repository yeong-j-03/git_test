package com.dormmatch.global.aop;

import com.dormmatch.domain.survey.entity.UserPreferences;
import com.dormmatch.domain.survey.repository.UserPreferencesRepository;
import com.dormmatch.domain.user.entity.Users;
import com.dormmatch.domain.user.repository.UsersRepository;
import com.dormmatch.global.exception.BusinessException;
import com.dormmatch.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class ValidationAspect {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UsersRepository usersRepository;

    @Autowired
    public ValidationAspect(UserPreferencesRepository userPreferencesRepository,
                            UsersRepository usersRepository){
        this.userPreferencesRepository = userPreferencesRepository;
        this.usersRepository = usersRepository;
    }

    // survey가 완료된 상태인지 검증
    // @RequiresSurvey를 메서드 앞에 붙여서 사용
    @Before("@annotation(com.dormmatch.global.aop.RequiresSurvey)")
    public void checkSurveyCompleted(JoinPoint joinPoint){
        Long userId = extractUserId(joinPoint);

        Boolean surveyCompleted = userPreferencesRepository
                .findByUserId(userId)
                .orElseThrow(()->new BusinessException(ErrorCode.SURVEY_NOT_FOUND))
                .getIsCompleted();

        if(surveyCompleted == false)
            throw new BusinessException(ErrorCode.SURVEY_NOT_FOUND);
    }


    // 로그인 여부를 확인하는 메서드
    // @RequiresAuth를 메서드 앞에 붙여 사용
    @Before("@annotation(requiresAuth)")
    public void checkAuth(JoinPoint joinPoint, RequiresAuth requiresAuth){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();

        log.debug("[AuthAspect] userId: {}, method: {}",
                userId, joinPoint.getSignature().getName());

        Users user = usersRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        AuthRole[] authRole = requiresAuth.roles();
        if (authRole.length == 0) return;

        boolean hasRole = Arrays.stream(authRole)
                .anyMatch(role -> role.name().equals(user.getRole()));

        if (!hasRole) {
            log.warn("[AuthAspect] 권한 없음 - userId: {}, role: {}, required: {}",
                    userId, user.getRole(), Arrays.toString(authRole));
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

    }


    // 인증서 인증
    // @RequiresCertification
    @Before("@annotation(com.dormmatch.global.aop.RequiresCertification)")
    public void checkCertification(JoinPoint joinPoint){
        Long userId = extractUserId(joinPoint);


    }

    // 매개변수로 들어오는 userId 확인
    public static Long extractUserId(JoinPoint joinPoint){
        for(Object args : joinPoint.getArgs()) {
            if (args instanceof Long) {
                return (Long) args;
            }
        }
        throw new IllegalStateException("userId를 찾을 수 없습니다.");
    }
}
