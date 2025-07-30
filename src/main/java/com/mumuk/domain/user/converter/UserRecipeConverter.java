package com.mumuk.domain.user.converter;


import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;

import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.entity.UserRecipe;

import java.util.*;
import java.util.stream.Collectors;

public class UserRecipeConverter {


    public static UserRecipeResponse.UserRecipeRes toUserRecipeRes(User user, Recipe recipe, UserRecipe userRecipe) {

        //레시피 재료 리스트: 레시피 재료 문자열 파싱
        String recipeIngredientsString = recipe.getIngredients();
        if (recipeIngredientsString == null || recipeIngredientsString.trim().isEmpty()) {
            recipeIngredientsString = "";
            }
        List<String> recipeIngredients = Arrays.stream(recipeIngredientsString.split(","))
                .map(String::trim)
                .filter(ingredient -> !ingredient.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 사용자가 보유한 재료 리스트
        List<String> inUserIngredients = user.getIngredients().stream()
                .map(ingredient -> {
                    String name = ingredient.getName();
                    return name;
                })
                .collect(Collectors.toList());

        //냉장고에 있는 재료 리스트
        List<String> inFridgeIngredients = new ArrayList<>();
        //냉장고에 없는 재료 리스트
        List<String> notInFridgeIngredients = new ArrayList<>();

        List<UserRecipeResponse.RecipeIngredientDTO> recipeIngredientDTOList = new ArrayList<>();

        for (String ingredient : recipeIngredients) {
            //{재료 이름, 냉장고에 있는지 여부}를 리스트에 저장
            boolean isInFridge = inUserIngredients.contains(ingredient);
            recipeIngredientDTOList.add(new UserRecipeResponse.RecipeIngredientDTO(ingredient, isInFridge));

            if (isInFridge) { //레시피 재료가 사용자가 보유한 재료에 있으면 있는 재료 리스트에 추가
                inFridgeIngredients.add(ingredient);
            } else { //레시피 재료가 사용자가 보유한 재료에 없으면 없는 재료 리스트에 추가
                notInFridgeIngredients.add(ingredient);
            }
        }

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
                recipe.getCategories().stream().map(Enum::name).collect(Collectors.toList()),
                recipe.getIngredients(),
                null, // sourceUrl은 현재 Recipe 엔티티에 없음
                recipeIngredientDTOList,
                inFridgeIngredients,
                notInFridgeIngredients,
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
