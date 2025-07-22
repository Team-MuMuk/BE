package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;

import java.util.List;

public interface SearchService {

    List<RecipeResponse.SimpleRes> SearchRecipeList(String keyword);

    RecipeResponse.DetailRes SearchDetailRecipe(Long recipeId);

}
