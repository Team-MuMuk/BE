package com.mumuk.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class NotificationRequest {

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FcmTokenReq {

        @NotBlank(message = "FCM 토큰은 필수입니다")
        @Size(min = 100, max = 200, message = "FCM 토큰 길이가 유효하지 않습니다")
        private String fcmToken;
    }
}
