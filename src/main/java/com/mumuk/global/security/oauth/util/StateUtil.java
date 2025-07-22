package com.mumuk.global.security.oauth.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StateUtil {

    public boolean isValidUUID(String state) {
        try {
            UUID.fromString(state);    // 파싱 시도 → 실패하면 예외
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
