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
    private LoginType loginType = LoginType.LOCAL;;

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

    /**
     * Constructs a new User with default values.
     */
    public User() {

    }

    /**
     * Updates the user's refresh token with the provided value.
     *
     * @param refreshToken the new refresh token to set
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Returns the unique identifier of the user.
     *
     * @return the user's ID
     */
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

    /**
     * Returns the user's full name.
     *
     * @return the name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user's nickname.
     *
     * @return the nickname of the user
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the user's phone number.
     *
     * @return the phone number associated with the user
     */
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

    /**
     * Sets the user's full name.
     *
     * @param name the full name to assign to the user
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
 * Sets the user's nickname.
 *
 * @param nickname the nickname to assign to the user
 */
public void setNickname(String nickname) {this.nickname = nickname;}

    /**
     * Sets the user's phone number.
     *
     * @param phone_number the phone number to assign to the user
     */
    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Sets the user's social login identifier.
     *
     * @param socialId the identifier associated with the user's social login account
     */
    public void setSocialId(String socialId) {
        this.socialId = socialId;
    }

    /**
     * Creates a new User instance with the specified name, nickname, email, and password.
     *
     * The returned User will have a null id and default values for other fields.
     *
     * @param name the user's full name
     * @param nickname the user's nickname
     * @param email the user's email address
     * @param password the user's password
     * @return a new User instance with the provided details
     */
    public static User of(String name, String nickname, String email, String password) {
        return new User(null, name, nickname, email, password);
    }

    /**
     * Constructs a User with the specified id, name, nickname, email, and password.
     *
     * @param id        the unique identifier for the user
     * @param name      the user's full name
     * @param nickname  the user's nickname
     * @param email     the user's email address
     * @param password  the user's password
     */
    public User(Long id, String name, String nickname, String email, String password) {
        this.id = id;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
    }

    /**
     * Returns the refresh token associated with the user.
     *
     * @return the user's refresh token, or null if not set
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token for the user.
     *
     * @param refreshToken the new refresh token value
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
