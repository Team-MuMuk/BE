package com.mumuk.domain.search.service;

import java.util.List;

public interface RecommendedRecipeService {

    // 1. 최근 검색한 레시피 불러오기 (가은님 개발파트)
    // 2. 해당 레시피의 카테고리 추출
    // 3. 카테고리 기반 검색, 리스트 반환받기 (recipeRepository)
    List<String> getRecommendedRecipeList(Long recipeId);

}
