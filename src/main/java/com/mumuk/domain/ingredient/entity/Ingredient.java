package com.mumuk.domain.ingredient.entity;


import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient")
public class Ingredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "재료 이름", nullable = false)
    private String name;

    @Column(name = "유통기한", nullable = false)
    private LocalDateTime expireDate;

    @Column(name = "재료 이미지")
    private String imageUrl;


    @Enumerated(EnumType.STRING)
    @Column(name = "디데이 알림 설정")
    private DdayFcmSetting daySetting;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Getter
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getExpireDate() {
        return expireDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public DdayFcmSetting getDaySetting() {
        return daySetting;
    }

    public User getUser() {
        return user;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExpireDate(LocalDateTime expireDate) {
        this.expireDate = expireDate;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDaySetting(DdayFcmSetting daySetting) {
        this.daySetting = daySetting;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
