package com.mumuk.domain.user.service;

import com.mumuk.domain.user.converter.TokenResponseConverter;
import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import com.mumuk.global.util.SmsUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.mumuk.global.apiPayload.code.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenResponseConverter tokenResponseConverter;
    private final SmsUtil smsUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, TokenResponseConverter tokenResponseConverter, SmsUtil smsUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenResponseConverter = tokenResponseConverter;
        this.smsUtil = smsUtil;
    }

    @Transactional
    public void signUp(AuthRequest.SignUpReq request) {

        validateRequest(request);

        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new AuthException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AuthException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.of(
                request.getName(),
                request.getNickname(),
                request.getPhoneNumber(),
                request.getLoginId(),
                encodedPassword
        );
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response) {

        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getPhoneNumber());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getPhoneNumber());

        user.updateRefreshToken(refreshToken);
        userRepository.save(user);          // 명시적 저장

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("X-Refresh-Token", refreshToken);

        return tokenResponseConverter.toResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
        User user = getUserFromToken(refreshToken);

        user.updateRefreshToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void withdraw(String accessToken) {
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }
        User user = getUserFromToken(accessToken);

        userRepository.delete(user);
    }

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String phoneNumber = jwtTokenProvider.getPhoneNumberFromToken(refreshToken);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        // 저장된 refreshToken이 없으면 재발급 불가
        String storedRefreshToken = user.getRefreshToken();
        if (storedRefreshToken == null || !jwtTokenProvider.validateToken(storedRefreshToken)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(phoneNumber);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(phoneNumber);

        // refreshToken 갱신
        user.updateRefreshToken(newRefreshToken);
        userRepository.save(user);

        return tokenResponseConverter.toResponse(newAccessToken, newRefreshToken);
    }

    public void findUserIdAndSendSms(AuthRequest.FindIdReq request) {

        User user = userRepository.findByNameAndPhoneNumber(request.getName(), request.getPhoneNumber())
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        String maskedId = maskUserId(user.getLoginId());
        String message = "[MuMuk] 요청하신 회원 id는 다음과 같습니다 : " + maskedId;

        smsUtil.sendOne(request.getPhoneNumber(), message);
    }

    private void validateRequest(AuthRequest.SignUpReq request) {

        if (!isValidNickname(request.getNickname())) {
            throw new AuthException(ErrorCode.INVALID_NICKNAME_FORMAT);
        }

        if(!isValidloginId(request.getLoginId())) {
            throw new AuthException(ErrorCode.INVALID_LOGIN_ID_FORMAT);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (!isValidPassword(request.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    // token -> Claim 객체 -> Subject 을 이용한 사용자 phoneNumber 추출
    private User getUserFromToken(String token) {
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        String category = claims.get("category", String.class);
        if (!"access".equals(category) && !"refresh".equals(category)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String phoneNumber = claims.getSubject();
        log.info("📱 phoneNumber from token: {}", phoneNumber);

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isValidNickname(String nickname) {
        return nickname != null && nickname.length() <= 10;
    }

    private boolean isValidloginId(String loginId) {
        String regex = "^(?=.*[A-Za-z])[A-Za-z\\d]{5,15}$";
        return Pattern.matches(regex, loginId);
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,15}$";
        return Pattern.matches(regex, password);
    }

    // id 다 안보여주고, * * 로 마스킹 처리되어 사용자에 반환
    private String maskUserId(String userId) {
        int visibleLength = userId.length() - 3;
        return userId.substring(0, visibleLength) + "***";
    }
}
