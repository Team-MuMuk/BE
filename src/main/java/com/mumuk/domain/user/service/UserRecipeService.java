package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.UserRecipeRequest;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;

public interface UserRecipeService {
    UserRecipeResponse.UserRecipeRes getUserRecipeDetail(Long userId, Long recipeId);
    UserRecipeResponse.RecentRecipeDTOList getRecentRecipes(Long userId);
    Long getMostRecentRecipeId(Long userId);
    UserRecipeResponse.LikedRecipeListDTO likedRecipe(Long userId, Integer page);//UserResponse.RecentRecipeDTO recentRecipe(Long userId);

    void clickLike(Long userId, UserRecipeRequest.ClickLikeReq req);
}


