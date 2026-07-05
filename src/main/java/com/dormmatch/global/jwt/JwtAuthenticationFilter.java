package com.dormmatch.global.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
// OncePerRequestFilter를 상속받아 사용자의 요청당 딱 한 번만 실행되도록 보장합니다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. HTTP 요청 헤더에서 JWT 토큰을 추출합니다.
        String token = resolveToken(request);

        // 2. 토큰이 존재하고, 위변조 및 만료 검증을 통과했다면 인증 처리를 진행합니다.
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject()); // 토큰 생성 시 넣었던 유저 ID 추출
            String role = claims.get("role", String.class); // 토큰 생성 시 넣었던 권한 추출

            // 3. Spring Security의 권한 체계에 맞게 "ROLE_USER"나 "ROLE_ADMIN" 형태로 권한 객체를 생성합니다.
            // 4. 인증용 객체(Authentication)를 생성합니다. (Principal: userId, Credentials: null, Authorities: 권한 리스트)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            // 5. SecurityContextHolder에 인증 객체를 저장합니다. 
            // 이렇게 저장해두어야 이후 컨트롤러나 서비스단에서 @AuthenticationPrincipal 등으로 유저 정보를 꺼낼 수 있고, 접근 제어가 가능해집니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. 다음 필터로 요청과 응답을 전달합니다. (토큰이 없거나 유효하지 않아도 다음 필터로 넘어가며, 인증이 필요한 API라면 시큐리티가 거부하게 됩니다.)
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP Request 헤더에서 'Authorization: Bearer <토큰>' 형태의 값을 찾아 토큰 문자열만 잘라내는 메서드
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        // 헤더 값이 존재하고 "Bearer "로 시작하는지 확인
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()); // "Bearer " 이후의 순수 토큰 값만 반환
        }

        return null; // 조건에 맞지 않으면 null 반환
    }
}