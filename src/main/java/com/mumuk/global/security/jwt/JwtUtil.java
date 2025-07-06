package com.mumuk.global.security.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

/**
 *  Jwt Category/userId/만료시간 추출
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private final JwtTokenProvider jwtTokenProvider;

    private JwtParser jwtParser;
    private Key secretKey;

    public JwtUtil(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
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
        return (int) (accessTokenValidity / 1000);   // JWT 유효 시간 형변환
    }

    public int getRefreshTokenValidity() {
        return (int) (refreshTokenValidity / 1000);  // JWT 유효 시간 형변환
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
