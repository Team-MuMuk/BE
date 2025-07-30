package com.mumuk.global.security.oauth.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.user.dto.response.KaKaoResponse;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.config.OAuthConfig;
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
    private final StateUtil stateUtil;
    private final OAuthConfig oAuthConfig;
    private String clientId;

    public KaKaoUtil(ObjectMapper objectMapper, StateUtil stateUtil, OAuthConfig oAuthConfig) {
        this.objectMapper = objectMapper;
        this.stateUtil = stateUtil;
        this.oAuthConfig = oAuthConfig;
    }

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private Set<String> allowedRedirectUris;

    @PostConstruct
    public void initAllowedRedirectUris() {
        this.clientId = oAuthConfig.getKakaoAppKey();
        this.allowedRedirectUris = new HashSet<>();
        allowedRedirectUris.add("http://localhost:8080/login/oauth2/code/kakao"); // 개발용
        allowedRedirectUris.add(redirectUri); // 운영 환경
        
        // Validate that required environment variables are set
        if (clientId == null || clientId.isEmpty()) {
            log.warn("Kakao app key is not configured. Kakao OAuth functionality will be disabled.");
        }
    }

    /**
     *  인가 코드를 이용해 카카오 서버로부터 OAuth 토큰 반환 받음.
     *  추후 OAuth Token 을 이용해, 카카오 서버로부터 사용자 정보 반환 => DB 저장 및 자체 인증/인가 로직
     */
    public KaKaoResponse.OAuthToken requestToken(String accessCode, String state, String redirectUri) {

        redirectUri = redirectUri.trim();

        if (!allowedRedirectUris.contains(redirectUri)) {
            log.error("[🚨ERROR🚨] 허용되지 않은 redirect_uri 요청: {}", redirectUri);
            throw new AuthFailureHandler(ErrorCode.KAKAO_INVALID_GRANT);
        }

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
        params.add("redirect_uri", redirectUri);
        params.add("code", accessCode);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);

        try {
            // 카카오 토큰 엔드포인트에 POST 요청
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kauth.kakao.com/oauth/token",
                    HttpMethod.POST,
                    kakaoTokenRequest,
                    String.class
            );

            log.info("[RESPONSE] 카카오 API 응답: {}", response.getBody());

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new AuthFailureHandler(ErrorCode.KAKAO_AUTH_FAILED);
            }

            // 정상 응답일 경우, 카카오 서버의 JSON 응답을 객체 형태로 역직렬화하여 반환
            return objectMapper.readValue(response.getBody(), KaKaoResponse.OAuthToken.class);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("[🚨ERROR🚨] 유효하지 않은 카카오 인증 코드 (401 Unauthorized)");
            throw new AuthFailureHandler(ErrorCode.KAKAO_INVALID_GRANT);
        } catch (JsonProcessingException e) {
            log.error("[🚨ERROR🚨] 카카오 응답 JSON 파싱 오류: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.KAKAO_JSON_PARSE_ERROR);
        } catch (Exception e) {
            log.error("[🚨ERROR🚨] 카카오 API 호출 중 오류 발생: {}", e.getMessage());
            throw new AuthFailureHandler(ErrorCode.KAKAO_API_ERROR);
        }
    }


    /**
     *  access token 을 사용해 카카오 사용자 정보 요청
     */
    public KaKaoResponse.KakaoProfile requestProfile(KaKaoResponse.OAuthToken oAuthToken) {

        if (oAuthToken == null || oAuthToken.getAccess_token() == null) {
                throw new AuthFailureHandler(ErrorCode.KAKAO_AUTH_FAILED);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.add("Authorization", "Bearer " + oAuthToken.getAccess_token());
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

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
