package com.mumuk.domain.notification.dto;

import lombok.*;

public class NotificationRequest {

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FcmTokenReq {
        private String fcmToken;
    }
}
