package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;

import java.util.List;

public interface SearchService {

    List<UserRecipeResponse.RecipeSummaryDTO> SearchRecipeList(Long userId, String keyword);

    RecipeResponse.DetailRes SearchDetailRecipe(Long recipeId);

}
