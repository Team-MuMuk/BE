package com.mumuk.domain.user.controller;


import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.service.AuthService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/sign-up")
    public Response<String> signUp(@Valid @RequestBody AuthRequest.SignUpReq request) {
        authService.signUp(request);
        return Response.ok(ResultCode.USER_SIGNUP_OK, "회원 가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public Response<TokenResponse> login(@Valid @RequestBody AuthRequest.LogInReq request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.logIn(request, response);
        return Response.ok(ResultCode.USER_LOGIN_OK, tokenResponse);
    }

    @PatchMapping("/logout")
    public Response<String> logout(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization");
        authService.logout(accessToken);
        return Response.ok(ResultCode.USER_LOGOUT_OK, "로그아웃이 완료되었습니다.");
    }

    @DeleteMapping("/withdraw")
    public Response<String> withdraw(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization");
        authService.withdraw(accessToken);
        return Response.ok(ResultCode.USER_WITHDRAW_OK, "회원 탈퇴가 완료되었습니다.");
    }


}
