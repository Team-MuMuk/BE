package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.mumuk.global.apiPayload.code.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public void signUp(AuthRequest.SingUpReq request) {

        validateRequest(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.of(
                request.getName(),
                request.getNickname(),
                request.getEmail(),
                encodedPassword
        );
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());


        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.setHeader("X-Refresh-Token", refreshToken);

        return new TokenResponse("Bearer " + accessToken, refreshToken);
    }

    @Transactional
    public void logout(String accessToken) {
        // 토큰에서 이메일 추출
        String email = jwtTokenProvider.getEmailFromToken(accessToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        // RefreshToken 제거
        user.updateRefreshToken(null);
        userRepository.save(user);
    }

    @Transactional
    public void withdraw(String accessToken) {
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        String token = accessToken.substring(7).trim();   // "Bearer " 제거
        String email = jwtTokenProvider.getEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }


    private void validateRequest(AuthRequest.SingUpReq request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        if (!isValidEmail(request.getEmail())) {
            throw new AuthException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (!isValidPassword(request.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(regex, email);
    }

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$";
        return Pattern.matches(regex, password);
    }
}
