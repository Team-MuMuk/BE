package com.mumuk.domain.recipe.dto.response;

import com.mumuk.domain.recipe.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

        public DetailRes(Recipe recipe) {
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SimpleRes {
        private Long id;
        private String title;
        private String recipeImage;
        private boolean liked;
    }

    @Getter
    @AllArgsConstructor
    public static class SimpleResList {
        private List<SimpleRes> simpleResList;
    }
}
