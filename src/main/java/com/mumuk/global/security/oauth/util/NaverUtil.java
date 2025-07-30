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
    private final StateUtil stateUtil;


    @Value("${naver.login.client-id:}")
    private String clientId;
    @Value("${naver.login.secret-key:}")
    private String clientSecret;


    public NaverUtil(ObjectMapper objectMapper, StateUtil stateUtil) {
        this.objectMapper = objectMapper;
        this.stateUtil = stateUtil;
        
        // Validate that required environment variables are set
        if (clientId == null || clientId.isEmpty()) {
            log.warn("Naver client ID is not configured. Naver OAuth functionality will be disabled.");
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            log.warn("Naver secret key is not configured. Naver OAuth functionality will be disabled.");
        }
    }

    /**
     *  인가 코드를 이용해 네이버 서버로부터 OAuth 토큰 반환 받음.
     *  추후 OAuth Token 을 이용해, 네이버 서버로부터 사용자 정보 반환 => DB 저장 및 자체 인증/인가 로직
     */
    public NaverResponse.OAuthToken requestToken(String accessCode, String state) {

        if (!stateUtil.isValidUUID(state)) {
            log.error("[🚨ERROR🚨] 유효하지 않은 state 값 (UUID 아님): {}", state);
            throw new AuthFailureHandler(ErrorCode.SOCIAL_LOGIN_INVALID_STATE);
        }

        // 요청 헤더 및 파라미터 구성
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret",clientSecret);
        params.add("code", accessCode);
        params.add("state",state);

        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = new HttpEntity<>(params, headers);

        try {
            // 네이버 토큰 엔드포인트에 POST 요청
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://nid.naver.com/oauth2.0/token",
                    HttpMethod.POST,
                    naverTokenRequest,
                    String.class
            );

            log.info("[RESPONSE] 네이버 API 응답: {}", response.getBody());

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new AuthFailureHandler(ErrorCode.NAVER_AUTH_FAILED);
            }

            // 정상 응답일 경우, 네이버 서버의 JSON 응답을 객체 형태로 역직렬화하여 반환
            return objectMapper.readValue(response.getBody(), NaverResponse.OAuthToken.class);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("[🚨ERROR🚨] 유효하지 않은 네이버 인증 코드 (401 Unauthorized)");
            throw new AuthFailureHandler(ErrorCode.NAVER_INVALID_GRANT);
        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 네이버 응답 JSON 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.NAVER_JSON_PARSE_ERROR);
        } catch (Exception e) {
            log.error("[🚨ERROR🚨] 네이버 API 호출 중 오류 발생: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.NAVER_API_ERROR);
        }
    }


    /**
     *  access token 을 사용해 네이버 사용자 정보 요청
     */
    public NaverResponse.NaverProfile requestProfile(NaverResponse.OAuthToken oAuthToken) {

        if (oAuthToken == null || oAuthToken.getAccess_token() == null) {
            throw new AuthFailureHandler(ErrorCode.NAVER_AUTH_FAILED);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.add("Authorization", "Bearer " + oAuthToken.getAccess_token());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            log.info("[INFO] 네이버 프로필 응답: {}", response.getBody());
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
