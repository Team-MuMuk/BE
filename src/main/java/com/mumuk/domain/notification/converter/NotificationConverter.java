package com.mumuk.domain.notification.converter;

import com.mumuk.domain.notification.dto.response.NotificationResponse;
import com.mumuk.domain.notification.entity.NotificationLog;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {
    public NotificationResponse.RecentRes toRecentAlarm(NotificationLog notificationLog){
        return new NotificationResponse.RecentRes(
                notificationLog.getId(),
                notificationLog.getTitle(),
                notificationLog.getBody(),
                notificationLog.getFcmMessageId(),
                notificationLog.getStatus(),
                notificationLog.getCreatedAt()
        );
    }

}
