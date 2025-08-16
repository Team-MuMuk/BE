package com.mumuk.domain.notification.dto.response;

import com.mumuk.domain.notification.entity.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationResponse {

    @Getter
    @AllArgsConstructor
    public static class RecentRes {
        private Long notificationLogid;
        private String title;
        private String body;
        private String messageId;
        private MessageStatus status;
        private LocalDateTime createdAt;
    }
}
