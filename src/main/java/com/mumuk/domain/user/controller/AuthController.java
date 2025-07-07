package com.mumuk.domain.user.controller;


import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import com.mumuk.domain.user.service.AuthServiceImpl;
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

    private final AuthServiceImpl authServiceImpl;

    /**
     * Constructs an AuthController with the specified authentication service implementation.
     *
     * @param authServiceImpl the authentication service used to handle user authentication operations
     */
    public AuthController(AuthServiceImpl authServiceImpl) {
        this.authServiceImpl = authServiceImpl;
    }

    /**
     * Registers a new user with the provided sign-up information.
     *
     * @param request the validated sign-up request containing user registration details
     * @return a response indicating successful user registration
     */
    @PostMapping("/sign-up")
    public Response<String> signUp(@Valid @RequestBody AuthRequest.SingUpReq request) {
        authServiceImpl.signUp(request);
        return Response.ok(ResultCode.USER_SIGNUP_OK, "회원 가입이 완료되었습니다.");
    }

    /**
     * Authenticates a user with the provided login credentials and returns authentication tokens.
     *
     * @param request the login request containing user credentials
     * @param response the HTTP response used to set authentication tokens
     * @return a response containing the generated authentication tokens upon successful login
     */
    @PostMapping("/login")
    public Response<TokenResponse> signUp(@Valid @RequestBody AuthRequest.LogInReq request, HttpServletResponse response) {
        TokenResponse tokenResponse = authServiceImpl.logIn(request, response);
        return Response.ok(ResultCode.USER_LOGIN_OK, tokenResponse);
    }

    /**
     * Logs out the currently authenticated user by invalidating the provided access token.
     *
     * @param request the HTTP request containing the Authorization header with the access token
     * @return a response indicating successful logout
     */
    @PatchMapping("/logout")
    public Response<String> logout(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization");
        authServiceImpl.logout(accessToken);
        return Response.ok(ResultCode.USER_LOGOUT_OK, "로그아웃이 완료되었습니다.");
    }

    /**
     * Handles user account withdrawal by deleting the authenticated user's account.
     *
     * Extracts the access token from the `Authorization` header of the request and invokes the withdrawal process.
     *
     * @param request the HTTP request containing the access token in the `Authorization` header
     * @return a response indicating successful user withdrawal
     */
    @DeleteMapping("/withdraw")
    public Response<String> withdraw(HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization");
        authServiceImpl.withdraw(accessToken);
        return Response.ok(ResultCode.USER_WITHDRAW_OK, "회원 탈퇴가 완료되었습니다.");
    }


}
