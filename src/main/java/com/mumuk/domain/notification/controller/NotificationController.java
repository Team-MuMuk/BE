package com.mumuk.domain.notification.controller;

import com.mumuk.domain.notification.dto.FcmTestRequest;
import com.mumuk.domain.notification.dto.NotificationRequest;
import com.mumuk.domain.notification.service.IngredientExpireScheduler;
import com.mumuk.domain.notification.service.NotificationService;
import com.mumuk.domain.user.dto.request.FCMRequest;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final IngredientExpireScheduler ingredientExpireScheduler;

    public NotificationController(NotificationService notificationService, IngredientExpireScheduler ingredientExpireScheduler) {
        this.notificationService = notificationService;
        this.ingredientExpireScheduler = ingredientExpireScheduler;
    }

    @Operation(summary = "푸시 알림 동의 API", description = "푸시 알림 동의 API 입니다.")
    @PostMapping("/pushAgree")
    public Response<String> pushAgree(@AuthUser Long userId, @RequestBody FCMRequest.pushAgreeReq req) {
        log.info("요청 값: {}", req.getFcmAgreed());
        Boolean pushAgree = notificationService.agreePush(userId, req);
        String responseMessage = pushAgree ? "푸시 알림 허용" : "푸시 알림 거부";
        return Response.ok(responseMessage);
    }

    @Operation(summary = "FCM 토큰 저장 API", description = "FCM 토큰 저장 API 입니다.")
    @PostMapping("/FcmToken")
    public Response<Long> saveOrUpdateFcmToken(@AuthUser Long userId, @RequestBody NotificationRequest.FcmTokenReq req) {
        notificationService.saveOrUpdateFcmToken(userId, req);
        return Response.ok(userId);
    }

    @Operation(summary = "FCM 테스트 API", description = "테스트용 API 입니다.")
    @PostMapping("/test")
    public Response<Void> testFCM(@AuthUser Long userId, @RequestBody FcmTestRequest req) {
        notificationService.testFcmService(userId, req.getFcmToken());
        return Response.ok();
    }

    @Operation(summary = "🔔 유통기한 알림 스케줄러 수동 실행", description = "유통기한 알림 스케줄러를 수동으로 테스트합니다.")
    @PostMapping("/send-expiry/test")
    public Response<String> triggerExpiryNotification() {
        ingredientExpireScheduler.sendExpiryNotifications();
        return Response.ok("🔔 스케줄러 수동 실행 완료");
    }
}
