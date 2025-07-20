package com.mumuk.domain.user.controller;


import com.mumuk.domain.user.converter.OAuthConverter;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.service.OAuthService;
import com.mumuk.global.apiPayload.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
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
    @PostMapping("/kakao-login")
    public Response<UserResponse.JoinResultDTO> kakaoLogin(@RequestParam("code") String accessCode, HttpServletResponse httpServletResponse) {
        User user = oAuthService.oAuthKaKaoLogin(accessCode, httpServletResponse);
        return Response.ok(OAuthConverter.toJoinResultDTO(user));
    }

    @Operation(summary = "네이버 로그인", description = "네이버 서버로부터 인가코드를 받아 이를 기반으로 로그인 처리")
    @PostMapping("/naver-login")
    public Response<UserResponse.JoinResultDTO> naverLogin(@RequestParam("code") String accessCode,@RequestParam("state") String state, HttpServletResponse httpServletResponse) {
        User user = oAuthService.oAuthNaverLogin(accessCode,state, httpServletResponse);
        return Response.ok(OAuthConverter.toJoinResultDTO(user));
    }
}
