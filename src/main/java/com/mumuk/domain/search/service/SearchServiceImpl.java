package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.recipe.service.RecipeService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;

import java.util.List;

public class SearchServiceImpl implements SearchService {

    private final RecipeRepository recipeRepository;
    private final TrendSearchService trendSearchService;
    private final RecipeService recipeService;

    public SearchServiceImpl(RecipeRepository recipeRepository, TrendSearchService trendSearchService, RecipeService recipeService) {
        this.recipeRepository = recipeRepository;
        this.trendSearchService = trendSearchService;
        this.recipeService = recipeService;
    }

    @Override
    public List<RecipeResponse.SimpleRes> SearchRecipeList(String keyword) {

        // 검색어가 존재한다면, 해당 검색어 조회수를 1 추가
        if (!(keyword == null || keyword.isEmpty())) {
            trendSearchService.increaseKeywordCount(keyword);
        }

        // 키워드를 바탕으로 결과값 반환
        List<RecipeResponse.SimpleRes> recipeList= recipeRepository.findByTitleContainingIgnoreCase(keyword);

        if (recipeList.isEmpty()) {
            throw new GlobalException(ErrorCode.SEARCH_RESULT_NOT_FOUND);
        }
        return recipeList;
    }

    @Override
    public RecipeResponse.DetailRes SearchDetailRecipe(Long recipeId) {
        return recipeService.getRecipeDetail(recipeId);
    }
}
