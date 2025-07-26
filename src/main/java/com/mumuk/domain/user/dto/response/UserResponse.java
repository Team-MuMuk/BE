package com.mumuk.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mumuk.domain.user.entity.UserRecipe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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

    @Getter
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileResultDTO {     // 프로필 수정 후 반환할 정보
        private String nickName;
        private String profileImage;

    }

    @Getter
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileInfoDTO { //프로필 조회 후 반환할 정보

        private String name;
        private String nickName;
        private String profileImage;
        private String statusMessage;

    }


}
