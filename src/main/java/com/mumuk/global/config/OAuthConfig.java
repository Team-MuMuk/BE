package com.mumuk.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuthConfig {

    @Value("${naver.login.client-id:}")
    private String naverClientId;

    @Value("${naver.login.secret-key:}")
    private String naverSecretKey;

    @Value("${kakao.native-app-key:}")
    private String kakaoAppKey;

    public String getNaverClientId() {
        return naverClientId;
    }

    public String getNaverSecretKey() {
        return naverSecretKey;
    }

    public String getKakaoAppKey() {
        return kakaoAppKey;
    }
} 