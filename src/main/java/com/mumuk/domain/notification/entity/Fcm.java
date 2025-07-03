package com.mumuk.domain.notification.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name="fcm")
public class Fcm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String fcmToken;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user id", nullable = false)
    private User user;



    // Getter
    public Long getId() {
        return id;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public User getUser() {
        return user;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
