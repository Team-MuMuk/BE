package com.mumuk.global.security.oauth.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.user.dto.response.KaKaoResponse;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthFailureHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Component
public class KaKaoUtil {

    private final ObjectMapper objectMapper;

    public KaKaoUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private Set<String> allowedRedirectUris;

    @PostConstruct
    public void initAllowedRedirectUris() {
        this.allowedRedirectUris = new HashSet<>();
        allowedRedirectUris.add("http://localhost:8080/login/oauth2/code/kakao"); // 개발용
        allowedRedirectUris.add(redirectUri); // 운영 환경
    }

    /**
     *  access token 을 사용해 카카오 사용자 정보 요청
     */
    public KaKaoResponse.KakaoProfile requestProfileWithAccessToken(String accessToken) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new AuthFailureHandler(ErrorCode.KAKAO_AUTH_FAILED);
            }

            log.info("[INFO] 카카오 프로필 응답: {}", response.getBody());
            return objectMapper.readValue(response.getBody(), KaKaoResponse.KakaoProfile.class);

        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 카카오 프로필 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.KAKAO_JSON_PARSE_ERROR);
        } catch (Exception e) {
            log.error("[🚨ERROR🚨] 카카오 프로필 요청 중 오류 발생: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.KAKAO_API_ERROR);
        }
    }
}