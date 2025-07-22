package com.mumuk.domain.user.service;

import com.mumuk.domain.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;

public interface OAuthService {
    User oAuthKaKaoLogin(String accessCode, String state,  HttpServletResponse response);

    User oAuthNaverLogin(String accessCode, String state, HttpServletResponse httpServletResponse);
}
