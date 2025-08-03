package com.mumuk.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.mumuk.domain.notification.entity.Fcm;
import com.mumuk.domain.notification.repository.NotificationRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
@Slf4j
public class FcmMessageService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public FcmMessageService(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;

    }

    @Transactional
    public boolean sendFcmMessage(Long userId, String title, String body ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getFcmAgreed()) {
            log.info("푸시 알림 비동의 상태: userId={}", userId);
            return false;
        }

        Fcm userToken = notificationRepository.findByUser(user).orElse(null);

        if (userToken == null || userToken.getFcmToken() == null || userToken.getFcmToken().isBlank()) {
            log.warn("FCM 토큰 없음 또는 유효하지 않음: userId={}", userId);
            return false;
        }

        String token = userToken.getFcmToken();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);   // FCM 서버에 메시지 전송
            log.info("FCM 전송 성공: userId={},  response={}", userId, response);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 전송 실패: userId={}, error={}", userId, e.getMessage(), e);

            Set<String> deletableErrorCodes = Set.of(
                    "unregistered",
                    "invalid_argument", "invalid_arguments",
                    "registration-token-not-registered",
                    "messaging/invalid-registration-token"
            );

            String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : null;
            if (errorCode != null && deletableErrorCodes.contains(errorCode.toLowerCase())) {
                notificationRepository.delete(userToken);
                log.warn("무효한 FCM 토큰 삭제: token={}, userId={}", token, user.getId());
                return false;
            }
            return false;
        }
    }
}
