package com.mumuk.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.response.BaseResponse;
import com.mumuk.global.security.handler.AuthFailureHandler;
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

    private final JwtUtil jwtUtil;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    private static final List<String> EXCLUDED_URLS = List.of(
            "/swagger-ui", "/v3/api-docs"
    );

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
                String email = jwtTokenProvider.extractEmail(token);

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new AuthFailureHandler(ErrorCode.USER_NOT_FOUND));

                JwtAuthenticationToken authentication = new JwtAuthenticationToken(user.getEmail());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 인증 성공하든, 토큰이 없어도 (인증 제외가 아니므로) 무조건 필터 통과 시도
            filterChain.doFilter(request, response);

        } catch (AuthFailureHandler e) {
            log.warn("[401] JWT 필터 인증 실패", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            BaseResponse<String> errorResponse = e.toResponse();
            String json = new ObjectMapper().writeValueAsString(errorResponse);
            response.getWriter().write(json);
        }
    }


    private boolean isExcluded(String requestURI) {
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;   // 헤더에 토큰이 없으면 null 반환
    }
}
