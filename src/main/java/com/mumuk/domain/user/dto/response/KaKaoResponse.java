package com.mumuk.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

public class KaKaoResponse {

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAuthToken {
        private String access_token;
        private String refresh_token;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoProfile {
        private Long id; // 소셜 ID

        private KakaoAccount kakao_account;

        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KakaoAccount {
            private String email;
            private Profile profile;

            @Getter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Profile {
                private String nickname;
                private String profile_image_url;
            }
        }
    }
}
