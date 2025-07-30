package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;

import java.util.List;

public interface RecipeRecommendService {
    
    List<RecipeResponse.DetailRes> recommendAndSaveRecipes(Long userId);
    // 식재료 기반 추천 (사용자 ID 기반)
    List<RecipeResponse.DetailRes> recommendByIngredient(Long userId);
    // 랜덤 추천
    List<RecipeResponse.DetailRes> recommendRandom();
} 