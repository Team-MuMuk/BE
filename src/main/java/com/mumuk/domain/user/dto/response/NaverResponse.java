package com.mumuk.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

public class NaverResponse {

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAuthToken {
        private String access_token;
        private String refresh_token;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NaverProfile {
        private String resultcode;
        private String message;
        private NaverProfileData response;

        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NaverProfileData {
            private String id;
            private String email;
            private String nickname;
            private String profile_image;

        }
    }
}
