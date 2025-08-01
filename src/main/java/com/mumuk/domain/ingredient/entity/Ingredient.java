package com.mumuk.domain.ingredient.entity;

import com.mumuk.domain.user.entity.User;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ingredient")
public class Ingredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "재료 이름", nullable = false)
    private String name;

    @Column(name = "유통기한", nullable = false)
    private LocalDate expireDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "재료별 알림 설정", joinColumns = @JoinColumn(name = "ingredient_id"))
    @Column(name = "디데이 알림 설정")
    private List<DdayFcmSetting> daySetting = new ArrayList<>(List.of(DdayFcmSetting.NONE));
    // 재료 등록시 알림설정을 하지 않으므로 기본값 NONE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // builder
    @Builder
    public Ingredient(String name, LocalDate expireDate, List<DdayFcmSetting> daySetting, User user) {
        this.name = name;
        this.expireDate = expireDate;
        this.daySetting = daySetting;
        this.user = user;
    }

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

    public List<DdayFcmSetting> getDaySetting() {
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

    public void setDaySetting(List<DdayFcmSetting> daySetting) {
        this.daySetting = daySetting;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
