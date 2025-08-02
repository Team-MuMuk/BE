package com.mumuk.domain.notification.service;

import com.mumuk.domain.notification.dto.NotificationRequest;
import com.mumuk.domain.user.dto.request.FCMRequest;

public interface NotificationService {
    Boolean agreePush(Long userId, FCMRequest.pushAgreeReq req);
    void saveOrUpdateFcmToken(Long userId, NotificationRequest.FcmTokenReq req);
    void testFcmService(Long userId, String targetToken);
}
