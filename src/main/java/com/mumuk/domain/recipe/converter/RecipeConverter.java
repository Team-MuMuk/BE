package com.mumuk.domain.recipe.converter;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;

public class RecipeConverter {

    // 등록용 DTO → 엔티티
    public static Recipe toRecipe(RecipeRequest.CreateReq req) {
        Recipe recipe = new Recipe();
        recipe.setTitle(req.getTitle());
        recipe.setRecipeImage(req.getRecipeImage());
        recipe.setDescription(req.getDescription());
        recipe.setCookingTime(req.getCookingTime());
        recipe.setCalories(req.getCalories());
        recipe.setProtein(req.getProtein());
        recipe.setCarbohydrate(req.getCarbohydrate());
        recipe.setFat(req.getFat());
        recipe.setCategory(RecipeCategory.valueOf(req.getCategory()));
        recipe.setSourceUrl(req.getSourceUrl());
        recipe.setIngredients(req.getIngredients());
        return recipe;
    }

    // 엔티티 → 상세 응답 DTO
    public static RecipeResponse.DetailRes toDetailRes(Recipe recipe) {
        return new RecipeResponse.DetailRes(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getRecipeImage(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                recipe.getCalories(),
                recipe.getProtein(),
                recipe.getCarbohydrate(),
                recipe.getFat(),
                recipe.getCategory().name(),
                recipe.getSourceUrl(),
                recipe.getIngredients()
        );
    }

    // 엔티티 → 간단한 응답 DTO
    public static RecipeResponse.SimpleRes toSimpleRes(Recipe recipe) {
        return new RecipeResponse.SimpleRes(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getRecipeImage()
        );
    }

}
