package com.mumuk.global.security.oauth.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.user.dto.response.KaKaoResponse;
import com.mumuk.domain.user.dto.response.NaverResponse;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthFailureHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Slf4j
@Component
public class NaverUtil {

    private final ObjectMapper objectMapper;

    public NaverUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     *  access token 을 사용해 네이버 사용자 정보 요청
     */
    public NaverResponse.NaverProfile requestProfileWithAccessToken(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            log.error("[🚨ERROR🚨] 네이버 Access Token 누락 또는 공백");
            throw new AuthFailureHandler(ErrorCode.NAVER_AUTH_FAILED);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            log.info("[INFO] 네이버 프로필 응답: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[🚨ERROR🚨] 네이버 프로필 응답 코드: {}", response.getStatusCode());
                throw new AuthFailureHandler(ErrorCode.NAVER_AUTH_FAILED);
            }

            return objectMapper.readValue(response.getBody(), NaverResponse.NaverProfile.class);

        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 네이버 프로필 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.NAVER_JSON_PARSE_ERROR);
        } catch (Exception e) {
            log.error("[🚨ERROR🚨] 네이버 프로필 요청 중 오류 발생: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.NAVER_API_ERROR);
        }
    }
}