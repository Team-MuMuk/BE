package com.mumuk.domain.user.converter;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.UserRecipe;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserRecipeConverter {

    public static UserRecipeResponse.UserRecipeRes toUserRecipeRes(Recipe recipe, UserRecipe userRecipe) {
        return new UserRecipeResponse.UserRecipeRes(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getRecipeImage(),
                recipe.getDescription(),
                recipe.getCookingTime(),
                recipe.getCalories(),
                recipe.getProtein(),
                recipe.getCarbohydrate(),
                recipe.getFat(),
                recipe.getCategory().name(),
                recipe.getSourceUrl(),
                recipe.getIngredients(),
                userRecipe.getViewed(),
                userRecipe.getViewedAt(),
                userRecipe.getLiked()
        );
    }

    public static UserRecipeResponse.RecentRecipeDTO toRecentRecipeDTO(Recipe recipe, boolean liked) {

        return UserRecipeResponse.RecentRecipeDTO.builder()
                .recipeId(recipe.getId())
                .name(recipe.getTitle())
                .imageUrl(recipe.getRecipeImage())
                .liked(liked)
                .build();
    }

    public static UserRecipeResponse.RecentRecipeDTOList toRecentRecipeDTOList(List<Long> recipeIds, Map<Long, Recipe> recipeMap,Map<Long, UserRecipe> userRecipeMap) {

        // 5. Redis에서 가져온 순서 (최신순)에 맞게 정렬하면서 DTO로 변환
        List<UserRecipeResponse.RecentRecipeDTO> sortedRecentRecipes = recipeIds.stream()
                .map(recipeId -> {
                    Recipe recipe = recipeMap.get(recipeId);
                    if (recipe == null) {
                        return null; // DB에 없는 레시피 ID는 스킵
                    }
                    // liked 정보 가져오기
                    boolean liked = Optional.ofNullable(userRecipeMap.get(recipeId))
                            .map(UserRecipe::getLiked)
                            .orElse(false); // 해당 UserRecipe가 없으면 기본값 false

                    return toRecentRecipeDTO(recipe,liked);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return UserRecipeResponse.RecentRecipeDTOList.builder()
                .recentRecipes(sortedRecentRecipes)
                .build();
    }
}
