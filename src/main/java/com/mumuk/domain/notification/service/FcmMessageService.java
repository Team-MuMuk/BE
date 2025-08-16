package com.mumuk.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.mumuk.domain.notification.entity.Fcm;
import com.mumuk.domain.notification.entity.MessageStatus;
import com.mumuk.domain.notification.entity.NotificationLog;
import com.mumuk.domain.notification.repository.NotificationLogRepository;
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
    private final NotificationLogRepository notificationLogRepository;

    public FcmMessageService(UserRepository userRepository, NotificationRepository notificationRepository, NotificationLogRepository notificationLogRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationLogRepository = notificationLogRepository;
    }
    // 로그 저장 트랜잭션과 메세지 전송 트랜잭션 분리
    @Transactional
    public NotificationLog createNotificationLog(Long userId, String title, String body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 푸시 알림 비동의 상태 + 스케줄러에서 1차로 ture인 사람에게만 알림을 전송하지만 2차 방어
        if (!user.getFcmAgreed()) {
            return(notificationLogRepository.save(new NotificationLog(title,body,MessageStatus.REJECTED,user)));
        }

        // 알림 로그 생성 및 저장
        NotificationLog notificationLog = new NotificationLog(title, body, MessageStatus.PENDING, user);
        return notificationLogRepository.save(notificationLog);
    }

    private Fcm isValidFcmToken(User user) {
        Fcm userToken = notificationRepository.findByUser(user).orElse(null);
        if (userToken == null || userToken.getFcmToken() == null || userToken.getFcmToken().isBlank()) {
            return null;
        }
        else{
            return userToken;
        }
    }

    @Transactional
    public boolean sendFcmMessage(NotificationLog notificationLog) {

        Fcm userToken = isValidFcmToken(notificationLog.getUser());
        if(userToken == null) {//토큰 유효성 검사
            notificationLog.setStatus(MessageStatus.FAILED);
            notificationLogRepository.save(notificationLog);
            log.warn("FCM 전송 스킵(토큰 없음/무효): notificationLogId={}, userId={}, messageStatus={}", notificationLog.getId(), notificationLog.getUser().getId(),MessageStatus.FAILED);
            return false;
        }
        if(notificationLog.getStatus() == MessageStatus.REJECTED) {
            log.warn("FCM 푸시 비동의 상태: notificationLogId={}, userId={}, messageStatus={}", notificationLog.getId(), notificationLog.getUser().getId(),MessageStatus.REJECTED);
            return false;
        }

        String token = userToken.getFcmToken();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(notificationLog.getTitle())
                        .setBody(notificationLog.getBody())
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            notificationLog.setStatus(MessageStatus.SENT);
            log.info("FCM 전송 성공: notificationLogId={}, response={}, messageStatus={}", notificationLog.getId(), response, notificationLog.getStatus());

            notificationLog.setFcmMessageId(response);
            notificationLogRepository.save(notificationLog);

            return true;
        } catch (FirebaseMessagingException e) {
            notificationLog.setStatus(MessageStatus.FAILED);
            log.warn("FCM 전송 실패: notificationLogId={}, error={}, messageStatus={}", notificationLog.getId(), e.getMessage(), notificationLog.getStatus(), e);

            notificationLogRepository.save(notificationLog);

            // 오류 처리 로직
            Set<String> deletableErrorCodes = Set.of(
                    "unregistered", "invalid_argument",
                    "invalid_arguments", "registration-token-not-registered",
                    "messaging/invalid-registration-token"
            );

            String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : null;
            if (errorCode != null && deletableErrorCodes.contains(errorCode.toLowerCase())) {
                notificationRepository.delete(userToken);
                log.warn("무효한 FCM 토큰 삭제: token={}, userId={}", token, notificationLog.getUser().getId());
                return false;
            }
            return false;
        }
    }
}

