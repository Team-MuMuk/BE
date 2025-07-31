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
        private List<String> categories; // 복수 카테고리 지원
        private String ingredients;

        public void setTitle(String title) { this.title = title; }
        public void setRecipeImage(String recipeImage) { this.recipeImage = recipeImage; }
        public void setDescription(String description) { this.description = description; }
        public void setCookingTime(Long cookingTime) { this.cookingTime = cookingTime; }
        public void setCalories(Long calories) { this.calories = calories; }
        public void setProtein(Long protein) { this.protein = protein; }
        public void setCarbohydrate(Long carbohydrate) { this.carbohydrate = carbohydrate; }
        public void setFat(Long fat) { this.fat = fat; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateReq {
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

        public void setTitle(String title) { this.title = title; }
        public void setRecipeImage(String recipeImage) { this.recipeImage = recipeImage; }
        public void setDescription(String description) { this.description = description; }
        public void setCookingTime(Long cookingTime) { this.cookingTime = cookingTime; }
        public void setCalories(Long calories) { this.calories = calories; }
        public void setProtein(Long protein) { this.protein = protein; }
        public void setCarbohydrate(Long carbohydrate) { this.carbohydrate = carbohydrate; }
        public void setFat(Long fat) { this.fat = fat; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    }


}
