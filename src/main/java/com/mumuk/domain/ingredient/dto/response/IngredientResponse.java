package com.mumuk.domain.ingredient.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class IngredientResponse {

    @Getter
    @AllArgsConstructor
    public static class RetrieveRes {
        private String name;
        private LocalDate expireDate;
        private LocalDateTime createdAt;
    }
}
