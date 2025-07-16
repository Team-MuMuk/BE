package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import java.util.List;
import com.mumuk.domain.recipe.entity.RecipeCategory;

public interface RecipeService {
    void createRecipe(RecipeRequest.CreateReq request);
    void deleteRecipe(Long id);
    RecipeResponse.DetailRes getRecipeDetail(Long id);
    List<String> findNamesByCategory(RecipeCategory category);
    List<RecipeResponse.DetailRes> getAllRecipes();
}