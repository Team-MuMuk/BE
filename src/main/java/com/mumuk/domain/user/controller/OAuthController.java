package com.mumuk.domain.user.controller;


import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.service.OAuthService;
import com.mumuk.global.apiPayload.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class OAuthController {

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @Operation(summary = "카카오 로그인", description = "카카오 서버로부터 인가코드를 받아 이를 기반으로 로그인 처리")
    @PostMapping("/kakao/callback")
    public Response<UserResponse.JoinResultDTO> kakaoLogin(@RequestParam("code") String accessCode, @RequestParam("state") String state) {
        UserResponse.JoinResultDTO result = oAuthService.oAuthKaKaoLogin(accessCode, state);
        return Response.ok(result);
    }

    @Operation(summary = "네이버 로그인", description = "네이버 서버로부터 인가코드를 받아 이를 기반으로 로그인 처리")
    @PostMapping("/naver/callback")
    public Response<UserResponse.JoinResultDTO> naverLogin(@RequestParam("code") String accessCode, @RequestParam("state") String state) {
        UserResponse.JoinResultDTO result = oAuthService.oAuthNaverLogin(accessCode, state);
        return Response.ok(result);
    }
}
