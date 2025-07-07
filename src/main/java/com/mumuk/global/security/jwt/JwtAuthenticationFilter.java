package com.mumuk.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.response.BaseResponse;
import com.mumuk.global.apiPayload.exception.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * Constructs a JwtAuthenticationFilter with the specified JWT token provider and user repository.
     *
     * @param jwtTokenProvider the provider used for validating and parsing JWT tokens
     * @param userRepository the repository used to retrieve user details by email
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    private static final List<String> EXCLUDED_URLS = List.of(
            "/swagger-ui", "/v3/api-docs"
    );

    /**
     * Processes incoming HTTP requests to enforce JWT-based authentication, except for excluded paths.
     *
     * For non-excluded requests, extracts and validates a JWT token from the Authorization header. If valid, retrieves the associated user and sets authentication in the security context. If authentication fails, responds with a 401 Unauthorized status and a JSON error message.
     *
     * @param request  the incoming HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs during processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 인증 제외 경로 필터를 통과
        if (isExcluded(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmailFromToken(token);

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

                JwtAuthenticationToken authentication = new JwtAuthenticationToken(user.getEmail());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 인증 성공하든, 토큰이 없어도 (인증 제외가 아니므로) 무조건 필터 통과 시도
            filterChain.doFilter(request, response);

        } catch (AuthException e) {
            log.warn("[401] JWT 필터 인증 실패", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            BaseResponse<String> errorResponse = e.toResponse();
            String json = new ObjectMapper().writeValueAsString(errorResponse);
            response.getWriter().write(json);
        }
    }


    /**
     * Determines whether the given request URI matches any of the excluded URL prefixes that bypass authentication.
     *
     * @param requestURI the URI of the incoming HTTP request
     * @return true if the request URI should bypass authentication; false otherwise
     */
    private boolean isExcluded(String requestURI) {
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }

    /**
     * Extracts the JWT token from the Authorization header of the given HTTP request.
     *
     * @param request the HTTP request from which to extract the token
     * @return the JWT token if present and prefixed with "Bearer ", or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;   // 헤더에 토큰이 없으면 null 반환
    }
}
