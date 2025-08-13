package com.mumuk.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.mumuk.domain.notification.converter.NotificationConverter;
import com.mumuk.domain.notification.dto.request.NotificationRequest;
import com.mumuk.domain.notification.dto.response.NotificationResponse;
import com.mumuk.domain.notification.entity.Fcm;
import com.mumuk.domain.notification.entity.MessageStatus;
import com.mumuk.domain.notification.repository.NotificationLogRepository;
import com.mumuk.domain.notification.repository.NotificationRepository;
import com.mumuk.domain.user.dto.request.FCMRequest;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository; // User Repository 필요
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final UserRepository userRepository; // 사용자 엔티티를 찾기 위함
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationConverter notificationConverter;

    public NotificationServiceImpl(UserRepository userRepository, NotificationRepository notificationRepository,
                                   NotificationLogRepository notificationLogRepository, NotificationConverter notificationConverter) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationConverter = notificationConverter;
    }

    @Override
    @Transactional
    public Boolean agreePush(Long userId, FCMRequest.pushAgreeReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setFcmAgreed(req.getFcmAgreed());
        return req.getFcmAgreed();
    }

    @Override
    @Transactional
    public void saveOrUpdateFcmToken(Long userId, NotificationRequest.FcmTokenReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        if (!user.getFcmAgreed()) { throw new BusinessException(ErrorCode.FCM_PUSH_NOT_AGREED);}

        Optional<Fcm> optionalFcm = notificationRepository.findByUser(user);
        Fcm fcm;

        if (optionalFcm.isPresent()) {
            fcm = optionalFcm.get();
            if (!req.getFcmToken().equals(fcm.getFcmToken())) {
                fcm.updateFcmToken(req.getFcmToken());
                log.info("✅ 기존 FCM 토큰이 갱신되었습니다.");
            } else {
                log.info("ℹ️ 기존 FCM 토큰이 이미 최신 상태입니다.");
            }

        } else {
            // 새로운 FCM 엔티티 생성
            fcm = new Fcm();
            fcm.setUser(user);
            fcm.setFcmToken(req.getFcmToken());
            log.info("✅ 새로운 FCM 토큰이 저장되었습니다.");
        }

        // 저장
        notificationRepository.save(fcm);
    }

    @Override
    public void testFcmService(Long userId, String fcmToken) {

        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("받은 FCM 토큰 값 : " + fcmToken);

        String title = "FCM 알림 테스트입니다.";
        String body = "테스트 성공했나요?";

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(notification)
                .build();

        log.info("📨 FCM 메시지 제목: {}", title);
        log.info("📨 FCM 메시지 내용: {}", body);

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 응답: {}", response);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 전송 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FCM_SEND_MESSAGE_ERROR);
        }
    }

    private static final int MAX_SIZE = 200;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse.RecentRes> getRecentAlarm(Long userId, int size) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime now  = LocalDateTime.now(zone);
        LocalDateTime from = now.minusDays(7);

        int limit = Math.max(1, Math.min(size, MAX_SIZE));
        Pageable pr = PageRequest.of(0, limit, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        //생성일시별로 내림차순 -> 타이 발생 -> 아이디순

        var statuses = List.of(
                MessageStatus.PENDING,
                MessageStatus.SENT
        );

        return notificationLogRepository.findByUserIdAndCreatedAtBetweenAndStatusIn(userId, from, now, statuses, pr)
                .stream()
                .map(notificationConverter::toRecentAlarm)
                .toList();
    }
}
