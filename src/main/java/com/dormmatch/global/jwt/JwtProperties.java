package com.dormmatch.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
//application.property의 jwt설정값을 java객체로 가져옴
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;
}
