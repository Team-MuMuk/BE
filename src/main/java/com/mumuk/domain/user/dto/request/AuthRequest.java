package com.mumuk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthRequest {


    @Getter
    @Setter
    @NoArgsConstructor
    public static class SignUpReq {
        @NotBlank
        private String name;

        @NotBlank
        private String nickname;

        @NotBlank
        private String phoneNumber;

        @NotBlank
        private String loginId;

        @NotBlank
        private String password;

        @NotBlank
        private String confirmPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LogInReq{
        @NotBlank
        private String loginId;

        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FindIdReq{
        @NotBlank
        private String name;

        @NotBlank
        private String phoneNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FindPassWordReq{
        @NotBlank
        private String loginId;

        @NotBlank
        private String name;

        @NotBlank
        private String phoneNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecoverPassWordReq {
        @NotBlank
        private String passWord;

        @NotBlank
        private String confirmPassWord;
    }
}
