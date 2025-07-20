package com.mumuk.domain.recipe.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

public class RecipeRequest {

    @Getter
    @NoArgsConstructor
    public static class CreateReq {
        private String title;
        private String recipeImage;
        private String description;
        private Long cookingTime;
        private Long calories;
        private Long protein;
        private Long carbohydrate;
        private Long fat;
        private String category; // enum 이름(예: "EXAMPLE")
        private String sourceUrl;
        private String ingredients;
    }
}
