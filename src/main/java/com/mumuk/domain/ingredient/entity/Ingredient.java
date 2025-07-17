package com.mumuk.domain.ingredient.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ingredient")
public class Ingredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "재료 이름", nullable = false)
    private String name;

    @Column(name = "유통기한", nullable = false)
    private LocalDate expireDate;

    //@Column(name = "재료 이미지")
    //private String imageUrl;


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

    public LocalDate getExpireDate() {
        return expireDate;
    }

    //public String getImageUrl() {
    //    return imageUrl;
    //}

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

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }

    //public void setImageUrl(String imageUrl) {
    //    this.imageUrl = imageUrl;
    //}

    public void setDaySetting(DdayFcmSetting daySetting) {
        this.daySetting = daySetting;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
