package com.mumuk.global.security.jwt;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.AuthException;
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
 *  Jwt 생성, 검증, 파싱 Provider
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private JwtParser jwtParser;
    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
    }

    // email 기반의 JWT 생성
    public String createAccessToken(String phoneNumber) {
        try {
            return generateToken(phoneNumber, accessTokenValidity,"access");
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    public String createRefreshToken(String phoneNumber) {
        try {
            return generateToken(phoneNumber, refreshTokenValidity, "refresh");
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    private String generateToken(String phoneNumber, long validity, String category) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(phoneNumber)
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
            throw new AuthException(ErrorCode.JWT_EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
    }

    // JWT 의 payload(Claims 객체) 추출
    public Claims parseClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public String getPhoneNumberFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject(); // subject 에 phoneNumber 가 들어 있음
    }

    public Claims getClaimsFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
    }

    // JWT 발급 시 필요한 시크릿 키를 Key 객체로 변환 (서명 검증)
    private Key getSigningKey() {
        return this.secretKey;
    }

}
