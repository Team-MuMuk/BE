package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import java.util.List;

public interface RecipeService {
    void createRecipe(RecipeRequest.CreateReq request);
    void deleteRecipe(Long id);
    void updateRecipe(Long id, RecipeRequest.UpdateReq request);
    RecipeResponse.DetailRes getRecipeDetail(Long id);
    List<String> findNamesByCategory(String category);
    List<String> findNamesByCategories(String categories);
    List<RecipeResponse.DetailRes> getAllRecipes();
    List<RecipeResponse.SimpleRes> getSimpleRecipes();
}
