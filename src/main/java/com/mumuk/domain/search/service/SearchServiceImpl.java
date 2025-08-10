package com.mumuk.domain.search.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.recipe.service.RecipeService;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.apiPayload.exception.GlobalException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final RecipeService recipeService;

    public SearchServiceImpl(RecipeRepository recipeRepository, UserRecipeRepository userRecipeRepository, RecipeService recipeService) {
        this.recipeRepository = recipeRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.recipeService = recipeService;
    }

    @Override
    public List<UserRecipeResponse.RecentRecipeDTO> SearchRecipeList(Long userId, String keyword) {

        if (keyword == null || keyword.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        // 키워드를 바탕으로 결과값 반환
        List<Recipe> recipes= recipeRepository.findByTitleContainingIgnoreCase(keyword);

        // 레시피가 없는 경우 예외 던지기
        if (recipes.isEmpty()) {
            throw new BusinessException(ErrorCode.SEARCH_RESULT_NOT_FOUND);
        }

        // 찜하기 여부를 불러오기 위해, 검색결과에서 반환받은 레시피 id를 바탕으로 userRecipe 생성
        List<UserRecipe> userRecipes =userRecipeRepository.findByUserIdAndRecipeIdIn(userId, recipes.stream().map(Recipe::getId).collect(Collectors.toList()));

        // dto 생성을 빠르게 하기 위해, recipeId를 키로, userRecipe를 밸류로 하는 map을 생성
        Map<Long, UserRecipe> userRecipeMap = userRecipes.stream()
                .collect(Collectors.toMap(userRecipe -> userRecipe.getRecipe().getId(), userRecipe -> userRecipe));

        // RecentRecipeDTO를 반환하는 List 생성
        List<UserRecipeResponse.RecentRecipeDTO> recipeList = recipes.stream()
                .map(recipe -> {
                    // 좋아요 여부 입력받기
                    UserRecipe userRecipe = userRecipeMap.get(recipe.getId());
                    boolean isLiked=(userRecipe!=null)&&Boolean.TRUE.equals(userRecipe.getLiked());
                    return new UserRecipeResponse.RecentRecipeDTO(recipe.getId(),recipe.getTitle(),recipe.getRecipeImage(),isLiked);
                }).collect(Collectors.toList());

        return recipeList;
    }

    @Override
    public RecipeResponse.DetailRes SearchDetailRecipe(Long recipeId) {
        return recipeService.getRecipeDetail(recipeId);
    }
}
