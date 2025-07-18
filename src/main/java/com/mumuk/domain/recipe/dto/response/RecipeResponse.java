package com.mumuk.domain.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

public class RecipeResponse {

    @Getter
    @AllArgsConstructor
    public static class DetailRes {
        private Long id;
        private String title;
        private String recipeImage;
        private String description;
        private Long cookingTime;
        private Long calories;
        private Long protein;
        private Long carbohydrate;
        private Long fat;
        private String category;
        private String sourceUrl;
        private String ingredients;

        @Getter
        @AllArgsConstructor
        public static class IngredientRes {
            private String name;
            private String amount;
        }
    }
}
