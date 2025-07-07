package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    void signUp(AuthRequest.SingUpReq request);
    TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response);
    void logout(String accessToken);
    void withdraw(String accessToken);
}
