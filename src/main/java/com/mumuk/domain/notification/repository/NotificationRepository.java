package com.mumuk.domain.notification.repository;

import com.mumuk.domain.notification.entity.Fcm;
import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Fcm, Long> {
    Optional<Fcm> findByUser(User user);
}
