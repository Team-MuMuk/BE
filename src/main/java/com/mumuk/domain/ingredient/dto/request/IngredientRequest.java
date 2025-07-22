package com.mumuk.domain.ingredient.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

public class IngredientRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterReq {
        @NotNull
        @JsonProperty("name")
        private String name;

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("expireDate")
        private LocalDate expireDate;

        @NotNull
        @JsonProperty("daySetting")
        private DdayFcmSetting daySetting;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateReq {

        @NotNull
        private String name;

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expireDate;

        @NotNull
        private DdayFcmSetting daySetting;
    }
}
