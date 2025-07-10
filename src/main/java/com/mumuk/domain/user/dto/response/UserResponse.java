package com.mumuk.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class UserResponse {

    @Getter
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinResultDTO {     // 가입 후 반환할 정보
        private String email;
        private String nickName;
        private String profileImage;
        private String refreshToken;
    }
}
