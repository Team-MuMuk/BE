package com.mumuk.domain.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.recipe.dto.response.RecipeBlogResponse;
import com.mumuk.domain.recipe.dto.response.RecipeCoupangResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.client.NaverBlogClient;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.util.TextUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.*;
import java.util.stream.Collectors;

/*
@Service
public class RecipeCoupangServiceImpl
        //extends RecipeCoupangService
{


    private final RecipeCoupangImageAsyncService recipeCoupangImageAsyncService;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public RecipeCoupangServiceImpl(RecipeCoupangImageAsyncService recipeCoupangImageAsyncService, UserRepository userRepository, RecipeRepository recipeRepository) {

        this.recipeCoupangImageAsyncService = recipeCoupangImageAsyncService;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    public RecipeCoupangResponse searchCoupang(Long userId, Long recipeId) {


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));

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

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");

            List<RecipeBlogResponse.Blog> coupangs = new ArrayList<>();

            // 파싱 결괏값 => DTO 변환
            for (JsonNode item : items) {
                String title = TextUtil.stripTags(item.path("title").asText());
                String rawDescription = Optional.ofNullable(item.path("description").asText()).orElse("");
                String description = TextUtil.smartTruncate(rawDescription, 70);
                String link = item.path("link").asText();

                // 캐시된 이미지 확인 및 비동기 크롤링 호출
                String cachedImage = recipeBlogImageAsyncService.getCachedImage(link);
                if (cachedImage==null) {
                    recipeBlogImageAsyncService.fetchAndCacheImage(link);
                }

                blogs.add(new RecipeBlogResponse.Blog(
                        title,
                        description,
                        link,
                        cachedImage     // null or 실제 URL
                ));
            }
            return new RecipeBlogResponse(blogs);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NAVER_API_PARSE_ERROR);
        }
    }
}
*/
