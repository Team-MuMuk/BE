package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.user.service.UserRecipeService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendedRecipeServiceImpl implements RecommendedRecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRecipeService userRecipeService;

    public RecommendedRecipeServiceImpl(RecipeRepository recipeRepository, UserRecipeService userRecipeService) {
        this.recipeRepository = recipeRepository;
        this.userRecipeService = userRecipeService;
    }

    // 가장 최근 조회한 레시피는 다른 사람이 맡은 개발부분, 아직 미구현


    @Override
    public List<String> getRecommendedRecipeList(Long userId) {

        Long recipeId= userRecipeService.getMostRecentRecipeId(userId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));

        RecipeCategory category = recipe.getCategory();

        // 레포지토리에서 random을 사용하기 때문에 naviveQuery를 사용,
        // nativeQuery 사용 과정에서 enum 변환에 문제가 생길 수 있기 때문에 카테고리 이름만 넘김
        List<Recipe> randomRecipeList = recipeRepository.findRandomRecipesByCategory(category.name(), recipeId);

        // recipeList에서 레시피 제목 (String)만 꺼내서 추출하려고 함
        // 이때 추천 검색어에는 항상 다른 레시피들이 추출되었으면 좋겠음

        List<String> recommendedRecipeList = randomRecipeList.stream()
                .map(Recipe::getTitle)
                .collect(Collectors.toList());

        return recommendedRecipeList;
    }


}
