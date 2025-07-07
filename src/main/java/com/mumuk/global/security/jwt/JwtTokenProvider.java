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

    /**
     * Initializes the cryptographic signing key and JWT parser using the configured secret.
     *
     * This method is automatically invoked after dependency injection to prepare the provider for token operations.
     */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build();
    }

    /**
     * Generates a JWT access token for the specified email.
     *
     * The token includes the email as the subject and a "category" claim set to "access".
     * Throws an AuthException with JWT_GENERATION_FAILED if token creation fails.
     *
     * @param email the email address to associate with the access token
     * @return the generated JWT access token as a String
     */
    public String createAccessToken(String email) {
        try {
            return generateToken(email, accessTokenValidity,"access");
        } catch (Exception e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    /**
     * Generates a refresh JWT token for the specified email.
     *
     * @param email the email address to associate with the refresh token
     * @return a signed JWT refresh token containing the email as the subject
     * @throws AuthException if token generation fails
     */
    public String createRefreshToken(String email) {
        try {
            return generateToken(email, refreshTokenValidity, "refresh");
        } catch (Exception e) {
            throw new AuthException(ErrorCode.JWT_GENERATION_FAILED);
        }
    }

    /**
     * Generates a JWT token for the specified email with a given validity period and category.
     *
     * @param email the email address to set as the token's subject
     * @param validity the duration in milliseconds for which the token is valid
     * @param category the token category, such as "access" or "refresh"
     * @return the generated JWT token as a compact string
     */
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

    /**
     * Validates the given JWT token's signature and expiration.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid
     * @throws AuthException if the token is expired or invalid
     */
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

    /**
     * Extracts and returns the claims (payload) from the provided JWT token.
     *
     * @param token the JWT token to parse
     * @return the claims contained within the token
     */
    public Claims parseClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    /**
     * Extracts the email (subject) from the given JWT token.
     *
     * If the token starts with the "Bearer " prefix, it is removed before parsing.
     *
     * @param token the JWT token, optionally prefixed with "Bearer "
     * @return the email address contained in the token's subject claim
     */
    public String getEmailFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();

        return claims.getSubject();
    }

    /**
     * Returns the cryptographic key used for signing and verifying JWT tokens.
     *
     * @return the secret key for JWT operations
     */
    private Key getSigningKey() {
        return this.secretKey;
    }

    /**
     * Determines whether the given JWT token is expired.
     *
     * @param token the JWT token to check
     * @return true if the token is expired; false otherwise
     */
    public boolean isExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Checks whether the provided JWT token is valid and not expired.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid; false otherwise
     */
    public boolean isValid(String token) {
        try {
            validateToken(token); // 내부적으로 예외 던짐
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
