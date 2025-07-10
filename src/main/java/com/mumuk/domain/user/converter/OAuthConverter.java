package com.mumuk.domain.user.converter;

import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.LoginType;
import com.mumuk.domain.user.entity.User;

public class OAuthConverter {

    public static User toUser(String email, String nickName, String profileImage, LoginType loginType, String socialId) {
        return User.of(email, nickName, profileImage, loginType, socialId);
    }

    public static UserResponse.JoinResultDTO toJoinResultDTO(User user) {
        return new UserResponse.JoinResultDTO(
                user.getEmail(),
                user.getNickName(),
                user.getProfileImage(),
                user.getRefreshToken()
        );
    }
}
