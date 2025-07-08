package com.mumuk.domain.user.controller;


import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.service.AuthService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import io.swagger.v3.oas.annotations.Operation;
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


    @Operation(summary = "회원 가입")
    @PostMapping("/sign-up")
    public Response<String> signUp(@Valid @RequestBody AuthRequest.SignUpReq request) {
        authService.signUp(request);
        return Response.ok(ResultCode.USER_SIGNUP_OK, "회원 가입이 완료되었습니다.");
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호 입력을 통해 로그인 후, Access Token 과 Refresh Token 이 반환됩니다.")
    @PostMapping("/login")
    public Response<TokenResponse> login(@Valid @RequestBody AuthRequest.LogInReq request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.logIn(request, response);
        return Response.ok(ResultCode.USER_LOGIN_OK, tokenResponse);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token 을 통해 검증 후 로그아웃이 진행됩니다.")
    @PatchMapping("/logout")
    public Response<String> logout(@RequestHeader("X-Refresh-Token") String refreshToken) {
        authService.logout(refreshToken);
        return Response.ok(ResultCode.USER_LOGOUT_OK, "로그아웃이 완료되었습니다.");
    }

    @Operation(summary = "회원 탈퇴", description = "Access Token 을 통해 사용자 인증을 검증 후 회원 탈퇴가 진행됩니다.")
    @DeleteMapping("/withdraw")
    public Response<String> withdraw(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization");
        authService.withdraw(accessToken);
        return Response.ok(ResultCode.USER_WITHDRAW_OK, "회원 탈퇴가 완료되었습니다.");
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token 을 통해 새로운 Access Token 과 Refresh Token 을 발급합니다.")
    @PostMapping("/reissue")
    public Response<TokenResponse> reissue(@RequestHeader("X-Refresh-Token") String refreshToken) {
        TokenResponse tokenResponse = authService.reissue(refreshToken);
        return Response.ok(ResultCode.TOKEN_REISSUE_OK, tokenResponse);
    }

    @Operation(summary = "Id 찾기", description = "이름과 휴대폰 번호 입력하여 SMS 메시지를 통해 아이디의 일부를 찾습니다.")
    @PatchMapping("/recover-id")
    public Response<String> findId(@Valid @RequestBody AuthRequest.FindIdReq request) {
        authService.findUserIdAndSendSms(request);
        return Response.ok(ResultCode.SEND_ID_BY_SMS_OK, "SMS 메시지로 ID의 일부를 확인하실 수 있습니다.");
    }

}
