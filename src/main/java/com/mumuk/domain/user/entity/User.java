package com.mumuk.domain.user.entity;


import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;


@Entity
@Table(name="users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LoginType loginType = LoginType.LOCAL;

    @Column(name = "알림 동의 여부", nullable = false)
    private Boolean isFcmAgreed = false;

    private String profileImage;

    @Column(nullable = false, unique = true)
    private String email;     // 자체&소셜 Id

    private String password;

    private String name;

    private String nickname;

    private String phone_number;

    private String statusMessage;

    @Column(name = "내부 식별을 위한 소셜 id")
    private String socialId;

    private String refreshToken;

    public User() {

    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public LoginType LoginType() {
        return loginType;
    }

    public Boolean getIsFcmAgreed() {
        return isFcmAgreed;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getSocialId() {
        return socialId;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setLoginType(LoginType loginType) {
        this.loginType = loginType;
    }

    public void setIsFcmAgreed(Boolean isFcmAgreed) {
        this.isFcmAgreed = isFcmAgreed;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNickname(String nickname) {this.nickname = nickname;}

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    public static User of(String name, String nickname, String email, String password) {
        return new User(null, name, nickname, email, password);
    }

    public User(Long id, String name, String nickname, String email, String password) {
        this.id = id;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
