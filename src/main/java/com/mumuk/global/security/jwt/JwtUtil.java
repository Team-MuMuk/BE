package com.mumuk.global.security.jwt;


import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 *  Jwt Category/userId/만료시간 추출
 */
@Slf4j
@Component
public class JwtUtil {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public JwtUtil(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public String extractCategory(String token) {
        return jwtTokenProvider.parseClaims(token).get("category", String.class);
    }

    // 토큰 만료 시간
    public long getRemainingSeconds(String token) {
        Date exp = jwtTokenProvider.parseClaims(token).getExpiration();
        return (exp.getTime() - System.currentTimeMillis()) / 1000;
    }

    public int getAccessTokenValidity() {
        return (int) (jwtProperties.getAccessTokenValidity() / 1000);   // JWT 유효 시간 형변환
    }

    public int getRefreshTokenValidity() {
        return (int) (jwtProperties.getRefreshTokenValidity() / 1000);  // JWT 유효 시간 형변환
    }

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            return claims.get("userId", Long.class);    // 또는 claims.getSubject() → Long.parseLong
        } catch (Exception e) {
            log.warn("[JWT] userId 추출 실패", e);
            return null;
        }
    }
}
