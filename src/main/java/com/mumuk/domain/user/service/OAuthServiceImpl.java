package com.mumuk.domain.user.service;


import com.mumuk.domain.user.converter.OAuthConverter;
import com.mumuk.domain.user.dto.response.KaKaoResponse;
import com.mumuk.domain.user.dto.response.NaverResponse;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.LoginType;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import com.mumuk.global.security.oauth.util.KaKaoUtil;
import com.mumuk.global.security.oauth.util.NaverUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;


@Slf4j
@Service
public class OAuthServiceImpl implements OAuthService {

    private final KaKaoUtil kakaoUtil;
    private final NaverUtil naverUtil;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;


    public OAuthServiceImpl(KaKaoUtil kakaoUtil, NaverUtil naverUtil, UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.kakaoUtil = kakaoUtil;
        this.naverUtil = naverUtil;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Override
    @Transactional
    public UserResponse.JoinResultDTO oAuthKaKaoLogin(String accessCode, String state) {

        KaKaoResponse.OAuthToken oAuthToken = kakaoUtil.requestToken(accessCode, state, kakaoRedirectUri);
        KaKaoResponse.KakaoProfile kakaoProfile = kakaoUtil.requestProfile(oAuthToken);

        String socialId = String.valueOf(kakaoProfile.getId());
        String email = kakaoProfile.getKakao_account().getEmail();
        String nickname = kakaoProfile.getKakao_account().getProfile().getNickname();
        String profileImage = kakaoProfile.getKakao_account().getProfile().getProfile_image_url();

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getLoginType() != LoginType.KAKAO) {
                        throw new AuthException(ErrorCode.ALREADY_REGISTERED_WITH_OTHER_LOGIN);
                    }
                    return existingUser;
                })
                .orElseGet(() -> createNewUser(email, nickname, profileImage, LoginType.KAKAO, socialId));

        String accessToken = jwtTokenProvider.createAccessTokenByEmail(user, user.getEmail(), LoginType.KAKAO);
        String refreshToken = jwtTokenProvider.createRefreshTokenByEmail(user, user.getEmail(), LoginType.KAKAO);

        user.setRefreshToken(refreshToken);
        user.setProfileImage(profileImage);
        userRepository.save(user);

        return new UserResponse.JoinResultDTO(
                user.getEmail(),
                user.getNickName(),
                user.getProfileImage(),
                accessToken,
                refreshToken
        );
    }

    @Override
    @Transactional
    public UserResponse.JoinResultDTO oAuthNaverLogin(String accessCode, String state) {

        NaverResponse.OAuthToken oAuthToken = naverUtil.requestToken(accessCode, state);
        NaverResponse.NaverProfile naverProfile = naverUtil.requestProfile(oAuthToken);

        String socialId = String.valueOf(naverProfile.getResponse().getId());
        String email = naverProfile.getResponse().getEmail();
        String nickname = naverProfile.getResponse().getNickname();
        String profileImage = naverProfile.getResponse().getProfile_image();

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getLoginType() != LoginType.NAVER) {
                        throw new AuthException(ErrorCode.ALREADY_REGISTERED_WITH_OTHER_LOGIN);
                    }
                    return existingUser;
                })
                .orElseGet(() -> createNewUser(email, nickname, profileImage, LoginType.NAVER, socialId));

        String accessToken = jwtTokenProvider.createAccessTokenByEmail(user, user.getEmail(), LoginType.NAVER);
        String refreshToken = jwtTokenProvider.createRefreshTokenByEmail(user, user.getEmail(), LoginType.NAVER);

        user.setRefreshToken(refreshToken);
        user.setProfileImage(profileImage);
        userRepository.save(user);

        return new UserResponse.JoinResultDTO(
                user.getEmail(),
                user.getNickName(),
                user.getProfileImage(),
                accessToken,
                refreshToken
        );
    }

    private User createNewUser(String email, String nickname, String profileImage, LoginType loginType, String socialId) {
        User newUser = OAuthConverter.toUser(email, nickname, profileImage, loginType, socialId);
        return userRepository.save(newUser);
    }
}
