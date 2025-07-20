package com.mumuk.domain.ingredient.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class IngredientResponse {

    @Builder
    public static class Res {
        private Long ingredientId;
        private String name;
        private LocalDate expireDate;
        private LocalDateTime createdAt;
    }
}
