package com.mumuk.domain.ingredient.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRegisterRequest {
    @NotNull
    @JsonProperty("name")
    private String name;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("expireDate")//유통기한 시간까지 받음
    private LocalDate expireDate;

    @NotNull
    @JsonProperty("daySetting")
    private DdayFcmSetting daySetting;
}

