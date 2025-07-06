package com.mumuk.global.security.jwt;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.handler.AuthFailureHandler;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 *  Jwt 생성, 검증, 파싱 Provider
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private JwtParser jwtParser;
    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
    }

    public JwtTokenProvider(JwtProperties jwtProperties) {this.jwtProperties = jwtProperties;}

    // email 기반의 JWT 생성
    public String createAccessToken(String email) {
        try {
            return generateToken(email, jwtProperties.getAccessTokenValidity(),"access");
        } catch (Exception e) {
            throw new AuthFailureHandler(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    public String createRefreshToken(String email) {
        try {
            return generateToken(email, jwtProperties.getRefreshTokenValidity(), "refresh");
        } catch (Exception e) {
            throw new AuthFailureHandler(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    private String generateToken(String email, long validity, String category) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(email)
                .claim("category", category)      // "access" 또는 "refresh"
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new AuthFailureHandler(ErrorCode.JWT_EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new AuthFailureHandler(ErrorCode.JWT_INVALID_TOKEN);
        }
    }

    // JWT 의 payload(Claims 객체) 추출
    public Claims parseClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    // JWT 발급 시 필요한 시크릿 키를 Key 객체로 변환 (서명 검증)
    private Key getSigningKey() {
        return this.secretKey;
    }

    public boolean isExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean isValid(String token) {
        try {
            validateToken(token); // 내부적으로 예외 던짐
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
