package com.mumuk.domain.healthManagement.dto.response;

import com.mumuk.domain.healthManagement.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class UserInfoResponse {

    @Getter
    @AllArgsConstructor
    public static class UserInfoRes {
        private Gender gender;
        private Long height;
        private Long weight;
    }

}
