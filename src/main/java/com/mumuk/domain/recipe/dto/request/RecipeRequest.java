package com.mumuk.domain.recipe.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

public class RecipeRequest {

    @Getter
    @NoArgsConstructor
    public static class CreateReq {
        private String name;
        private String description;
        private Long cookingMinutes;
        private Long protein;
        private Long carbohydrate;
        private Long fat;
        private Long calories;
        private String category; // enum 이름(예: "EXAMPLE")
        private List<String> imageUrls; // 이미지 URL 리스트
        private List<IngredientReq> ingredients; // 재료 리스트

        @Getter
        @NoArgsConstructor
        public static class IngredientReq {
            private String name;
            private String amount;
        }
    }
}