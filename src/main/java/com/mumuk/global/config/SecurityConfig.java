package com.mumuk.global.config;

import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.security.jwt.JwtAuthenticationFilter;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * Creates and returns a {@link JwtAuthenticationFilter} configured with the application's JWT token provider and user repository.
     *
     * @return a JwtAuthenticationFilter for handling JWT-based authentication
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    }

    /**
     * Creates a bean for password encoding using the BCrypt hashing algorithm.
     *
     * @return a PasswordEncoder that uses BCrypt for hashing passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures and returns the application's security filter chain.
     *
     * Enables CORS, disables CSRF protection, sets session management to stateless, and permits all incoming requests, including static resources and Swagger endpoints. The JWT authentication filter is defined but not added to the filter chain.
     *
     * @param http the {@link HttpSecurity} to modify
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configure(http))       // CORS 설정 활성화
                .csrf(AbstractHttpConfigurer::disable)   // CSRF 비활성화
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/auth/**").permitAll() // 인증 없이 접근 허용
                        .requestMatchers("/index.html", "/static/**", "/favicon.ico").permitAll() // 정적 파일 허용
                        .requestMatchers("/swagger", "/swagger/", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger 허용
                        .requestMatchers("/**").permitAll()     // 모든 요청 허용 (테스트용)
                );
//                .addFilterBefore(jwtAuthenticationFilter());
        return http.build();
    }
}
