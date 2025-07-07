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

    /**
     * Constructs an AuthServiceImpl with the specified user repository, password encoder, and JWT token provider.
     */
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new user after validating the sign-up request and ensuring email uniqueness.
     *
     * Throws an {@link AuthException} with {@link ErrorCode#EMAIL_ALREADY_EXISTS} if the email is already registered.
     * The password is securely encoded before saving the user.
     */
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

    /**
     * Authenticates a user with the provided login credentials, generates JWT access and refresh tokens, updates the user's refresh token, and sets the tokens in the HTTP response headers.
     *
     * @param request the login request containing user credentials
     * @param response the HTTP response to which authentication tokens are added as headers
     * @return a TokenResponse containing the generated access and refresh tokens
     * @throws AuthException if the user is not found or the password does not match
     */
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

    /**
     * Logs out the user associated with the provided access token by removing their refresh token.
     *
     * @param accessToken the JWT access token used to identify the user
     * @throws AuthException if the user corresponding to the token is not found
     */
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

    /**
     * Deletes the user account associated with the provided access token.
     *
     * @param accessToken the JWT access token prefixed with "Bearer "
     * @throws AuthException if the token is invalid or the user is not found
     */
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


    /**
     * Validates the fields of a sign-up request, ensuring nickname, email, and password meet required formats and that passwords match.
     *
     * @param request the sign-up request containing user registration details
     * @throws AuthException if any field fails validation, with an appropriate error code
     */
    private void validateRequest(AuthRequest.SingUpReq request) {

        if (!isValidNickname(request.getNickname())) {
            throw new AuthException(ErrorCode.INVALID_NICKNAME_FORMAT);
        }

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

    /**
     * Checks if the provided nickname is non-null and does not exceed 10 characters.
     *
     * @param nickname the nickname to validate
     * @return true if the nickname is valid, false otherwise
     */
    private boolean isValidNickname(String nickname) {
        return nickname != null && nickname.length() <= 10;
    }

    /**
     * Checks if the provided email string matches a standard email format.
     *
     * @param email the email address to validate
     * @return true if the email is in a valid format, false otherwise
     */
    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(regex, email);
    }

    /**
     * Checks if the provided password meets the required complexity rules.
     *
     * The password must be 8 to 15 characters long and include at least one letter, one digit, and one special character from the set !@#$%^&*()_+=-.
     *
     * @param password the password string to validate
     * @return true if the password is valid according to the complexity rules; false otherwise
     */
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,15}$";
        return Pattern.matches(regex, password);
    }
}
