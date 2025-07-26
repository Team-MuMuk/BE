package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;

public interface RecipeRecommendService {
    
    /**
     * 사용자의 재료와 알러지 정보를 기반으로 AI가 5개의 레시피를 추천하고 Recipe 엔티티에 저장합니다.
     */
    RecipeResponse.AiRecommendListDto recommendAndSaveRecipes(RecipeRequest.AiRecommendReq request);
} 