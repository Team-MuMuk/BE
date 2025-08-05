package com.mumuk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserRequest {


    @Getter
    @Setter
    @NoArgsConstructor
    public static class EditProfileReq{ //프로필 정보 등록, 프로필 정보 수정

        @NotBlank
        private String name;

        @NotBlank
        private String nickName;

        @NotBlank
        private String profileImage;

        @NotBlank
        private String statusMessage;
    }
}
