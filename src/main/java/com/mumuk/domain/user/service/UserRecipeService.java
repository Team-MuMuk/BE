package com.mumuk.domain.user.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;

import java.util.List;

public interface UserRecipeService {

    RecipeResponse.DetailRes getUserRecipeDetail(Long userId, Long recipeId);
    RecipeResponse.SimpleResList getRecentRecipes(Long userId);
    Long getMostRecentRecipeId(Long userId);
}


