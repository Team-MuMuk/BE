package com.mumuk.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

public class FCMRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class pushAgreeReq {

        @NotNull
        @JsonProperty("fcmAgreed")
        private Boolean fcmAgreed;  // 푸시알림 동의 여부 true / false
    }
}
