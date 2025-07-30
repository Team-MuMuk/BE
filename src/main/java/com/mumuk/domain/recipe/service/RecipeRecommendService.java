package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.global.apiPayload.exception.BusinessException;

import java.util.List;

/**
 * AI 기반 레시피 추천 서비스
 */
public interface RecipeRecommendService {
    
    /**
     * 사용자의 재료를 기반으로 레시피를 추천하고 저장
     * @param userId 사용자 ID
     * @return 추천된 레시피 목록
     * @throws BusinessException AI 추천 실패 시
     */
    List<RecipeResponse.DetailRes> recommendAndSaveRecipesByIngredient(Long userId);
    
    /**
     * 사용자의 재료를 기반으로 레시피 추천 (저장하지 않음)
     * @param userId 사용자 ID
     * @return 추천된 레시피 목록
     * @throws BusinessException AI 추천 실패 시
     */
    List<RecipeResponse.DetailRes> recommendByIngredient(Long userId);
    
    /**
     * 랜덤 레시피 추천
     * @return 추천된 레시피 목록
     * @throws BusinessException AI 추천 실패 시
     */
    List<RecipeResponse.DetailRes> recommendRandom();
} 