package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    /**
 * Registers a new user account using the provided sign-up request data.
 *
 * @param request the sign-up request containing user registration details
 */
void signUp(AuthRequest.SingUpReq request);
    /**
 * Authenticates a user based on the provided login request and returns authentication tokens.
 *
 * @param request the login request containing user credentials
 * @param response the HTTP servlet response to which authentication details may be added
 * @return a TokenResponse containing authentication tokens upon successful login
 */
TokenResponse logIn(AuthRequest.LogInReq request, HttpServletResponse response);
    /**
 * Logs out the user associated with the provided access token, invalidating the session or token.
 *
 * @param accessToken the access token identifying the user session to be terminated
 */
void logout(String accessToken);
    /**
 * Processes user account withdrawal using the provided access token.
 *
 * @param accessToken the access token identifying the user to withdraw
 */
void withdraw(String accessToken);
}
