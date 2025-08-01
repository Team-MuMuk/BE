package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;

public interface OAuthService {
    UserResponse.JoinResultDTO oAuthKaKaoLogin(String accessCode, String state);
    UserResponse.JoinResultDTO oAuthNaverLogin(String accessCode, String state);
}
