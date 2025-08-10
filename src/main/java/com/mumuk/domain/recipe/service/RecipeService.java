package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import java.util.List;

public interface RecipeService {
    void createRecipe(RecipeRequest.CreateReq request);
    void deleteRecipe(Long id);
    void updateRecipe(Long id, RecipeRequest.CreateReq request);
    RecipeResponse.DetailRes getRecipeDetail(Long id);
    List<String> findNamesByCategory(String category);
    List<String> findNamesByCategories(String categories);
    List<RecipeResponse.DetailRes> getAllRecipes();
    List<RecipeResponse.SimpleRes> getSimpleRecipes(Long userId);
    
    // 레시피 재료 매칭 기능 (토큰 기반 인증)
    RecipeResponse.IngredientMatchingRes matchIngredientsByAI(Long recipeId);
    RecipeResponse.IngredientMatchingRes matchIngredientsSimple(Long recipeId);
}
