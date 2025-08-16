package com.mumuk.domain.notification.repository;

import com.mumuk.domain.notification.entity.MessageStatus;
import com.mumuk.domain.notification.entity.NotificationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByUserIdAndCreatedAtBetweenAndStatusIn(
            Long userId,
            LocalDateTime from,
            LocalDateTime to,
            Collection<MessageStatus> statuses,
            Pageable pageable
    );

}
