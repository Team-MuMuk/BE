package com.mumuk.domain.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

public class RecipeResponse {

    @Getter
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private String name;
        private String description;
        private Long cookingMinutes;
        private Long protein;
        private Long carbohydrate;
        private Long fat;
        private Long calories;
        private String category;
        private List<String> imageUrls;
        private List<IngredientRes> ingredients;

        @Getter
        @AllArgsConstructor
        public static class IngredientRes {
            private String name;
            private String amount;
        }
    }
}