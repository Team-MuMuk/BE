package com.mumuk.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserRequest {


    @Getter
    @Setter
    @NoArgsConstructor
    public static class EditProfileReq{ //프로필 정보 등록, 프로필 정보 수정

        @NotNull
        private String name;

        @NotNull
        private String nickName;

        @NotNull
        private String profileImage;

        @NotNull
        private String statusMessage;
    }
}
