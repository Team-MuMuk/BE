package com.mumuk.domain.recipe.dto.response;

import com.mumuk.domain.recipe.entity.Recipe;
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
        private List<String> categories;
        private String ingredients;
        
        /**
         * Recipe 엔티티로부터 DetailRes를 생성합니다.
         * @param recipe Recipe 엔티티
         * @param isLiked 좋아요 여부 (현재는 사용하지 않음)
         * @return DetailRes 객체
         */
        public static DetailRes from(Recipe recipe, boolean isLiked) {
            List<String> categoryNames = recipe.getCategories().stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
                
            return new DetailRes(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getRecipeImage(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                recipe.getCalories(),
                recipe.getProtein(),
                recipe.getCarbohydrate(),
                recipe.getFat(),
                categoryNames,
                recipe.getIngredients()
            );
        }
    }



    @Getter
    @AllArgsConstructor
    public static class IngredientMatchingRes {
        private Long recipeId;
        private String recipeTitle;
        private List<String> match;
        private List<String> mismatch;
        private List<ReplaceableIngredient> replaceable;
    }

    @Getter
    @AllArgsConstructor
    public static class ReplaceableIngredient {
        private String recipeIngredient;
        private String userIngredient;
    }
}
