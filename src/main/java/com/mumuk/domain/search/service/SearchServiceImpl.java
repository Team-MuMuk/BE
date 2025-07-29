package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.recipe.service.RecipeService;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final TrendSearchService trendSearchService;
    private final RecipeService recipeService;

    public SearchServiceImpl(RecipeRepository recipeRepository, UserRecipeRepository userRecipeRepository, TrendSearchService trendSearchService, RecipeService recipeService) {
        this.recipeRepository = recipeRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.trendSearchService = trendSearchService;
        this.recipeService = recipeService;
    }

    @Override
    public List<UserRecipeResponse.RecentRecipeDTO> SearchRecipeList(Long userId, String keyword) {

        // 검색어가 존재한다면, 해당 검색어 조회수를 1 추가
        if (!(keyword == null || keyword.isEmpty())) {
            trendSearchService.increaseKeywordCount(keyword);
        }

        // 키워드를 바탕으로 결과값 반환
        List<RecipeResponse.SimpleRes> recipes= recipeRepository.findByTitleContainingIgnoreCase(keyword);

        if (recipes.isEmpty()) {
            throw new GlobalException(ErrorCode.SEARCH_RESULT_NOT_FOUND);
        }

        List<UserRecipe> userRecipes =userRecipeRepository.findByUserIdAndRecipeIdIn(userId, recipes.stream().map(RecipeResponse.SimpleRes::getId).collect(Collectors.toList()));

        Map<Long, UserRecipe> userRecipeMap = userRecipes.stream()
                .collect(Collectors.toMap(userRecipe -> userRecipe.getRecipe().getId(), userRecipe -> userRecipe));

        List<UserRecipeResponse.RecentRecipeDTO> recipeList = recipes.stream()
                .map(recipe -> {
                    UserRecipe userRecipe = userRecipeMap.get(recipe.getId());
                    boolean isLiked=(userRecipe!=null)&&userRecipe.getLiked();
                    return new UserRecipeResponse.RecentRecipeDTO(recipe.getId(),recipe.getRecipeImage(),recipe.getTitle(),isLiked);
                }).collect(Collectors.toList());

        return recipeList;
    }

    @Override
    public RecipeResponse.DetailRes SearchDetailRecipe(Long recipeId) {
        return recipeService.getRecipeDetail(recipeId);
    }
}
