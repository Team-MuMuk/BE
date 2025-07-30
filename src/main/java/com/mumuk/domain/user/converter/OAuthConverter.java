package com.mumuk.domain.user.converter;

import com.mumuk.domain.user.entity.LoginType;
import com.mumuk.domain.user.entity.User;

public class OAuthConverter {

    public static User toUser(String email, String nickName, String profileImage, LoginType loginType, String socialId) {
        return User.of(email, nickName, profileImage, loginType, socialId);
    }
}
