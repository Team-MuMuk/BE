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
        private String email;

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
        private String email;

        @NotBlank
        private String password;
    }

}
