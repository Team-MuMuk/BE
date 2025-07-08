package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    void signUp(AuthRequest.SignUpReq request);
    TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response);
    void logout(String refreshToken);
    void withdraw(String accessToken);
    TokenResponse reissue(String refreshToken);
    void findUserIdAndSendSms(AuthRequest.FindIdReq request);
    void findUserPassWordAndSendSms(AuthRequest.FindPassWordReq request);
    void reissueUserPassword(AuthRequest.RecoverPassWordReq request, String accessToken);
}
