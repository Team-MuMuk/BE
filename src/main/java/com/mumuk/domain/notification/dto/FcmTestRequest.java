package com.mumuk.domain.notification.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmTestRequest {
    private String fcmToken;

    public FcmTestRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
