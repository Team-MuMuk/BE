package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.entity.LoginType;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    void signUp(AuthRequest.SignUpReq request);
    TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response);
    void logout(String refreshToken, LoginType loginType);
    void withdraw(Long userId);
    TokenResponse reissue(String refreshToken, LoginType loginType);
    void findUserIdAndSendSms(AuthRequest.FindIdReq request);
    void findUserPassWordAndSendSms(AuthRequest.FindPassWordReq request);
    void reissueUserPassword(AuthRequest.RecoverPassWordReq request, Long userId);
}
