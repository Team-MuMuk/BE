package com.mumuk.domain.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    }

    @Getter
    @AllArgsConstructor
    public static class SimpleRes {
        private Long id;
        private String title;
        private String recipeImage;
    }

    @Getter
    @AllArgsConstructor
    public static class SimpleResList {
        private List<SimpleRes> simpleResList;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecommendDto {
        private String title;
        private String description;
        private String ingredients;
        private String cookingSteps;
        private Long estimatedCookingTime;
        private Long estimatedCalories;
        private Long estimatedProtein;
        private Long estimatedCarbohydrate;
        private Long estimatedFat;
        private String difficulty;
        private String category;
        private String reason;
        private String nutritionNotes;
        private String introMent;
        private String mainIngredients;
        private String nutritionInfo;
        private String cookingInfo;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecommendListDto {
        private List<AiRecommendDto> recommendations;
        private String summary;
    }
}
