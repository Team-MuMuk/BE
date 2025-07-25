package com.mumuk.domain.user.service;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.user.converter.UserRecipeConverter;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.apiPayload.exception.GlobalException;
import com.mumuk.global.security.exception.AuthException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mumuk.domain.user.converter.UserRecipeConverter.toRecentRecipeDTOList;

@Slf4j
@Service
public class UserRecipeServiceImpl implements UserRecipeService{

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final RecentRecipeService recentRecipeService;

    public UserRecipeServiceImpl(StringRedisTemplate redisTemplate, UserRepository userRepository, RecipeRepository recipeRepository, UserRecipeRepository userRecipeRepository, RecentRecipeService recentRecipeService) {

        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.recentRecipeService = recentRecipeService;
    }

    @Override
    @Transactional
    public UserRecipeResponse.UserRecipeRes getUserRecipeDetail(Long userId, Long recipeId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        //사용자와 레시피가 유효하면 redis에 데이터 저장
        recentRecipeService.addRecentRecipe(userId, recipeId);
        UserRecipe userRecipe = userRecipeRepository.findByUserIdAndRecipeId(userId, recipeId)
                .map(existing -> {
                    //사용자가 해당 레시피를 조회한 적이 있으면
                    //조회 시간을 현재 시간으로 변경
                    existing.setViewed(true);
                    existing.setViewedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> {
                    //조회한 적이 없으면 데이터를 저장
                    UserRecipe newUserRecipe = new UserRecipe();
                    newUserRecipe.setUser(user);
                    newUserRecipe.setRecipe(recipe);
                    newUserRecipe.setViewed(true);
                    newUserRecipe.setViewedAt(LocalDateTime.now());
                    newUserRecipe.setLiked(false);
                    return userRecipeRepository.save(newUserRecipe);
                });
        return UserRecipeConverter.toUserRecipeRes(recipe, userRecipe);
    }

    @Override
    public UserRecipeResponse.RecentRecipeDTOList getRecentRecipes(Long userId) {
        String key = "user:" + userId + ":recent_recipes";

        // Redis에서 최신순으로 recipeId 목록을 조회
        Set<String> recipeIdsAsString = redisTemplate.opsForZSet().reverseRange(key, 0, 7);

        if (recipeIdsAsString == null || recipeIdsAsString.isEmpty()) {
            return new UserRecipeResponse.RecentRecipeDTOList(Collections.emptyList());

        }

        // recipeId 타입 변환(String > Long)
        List<Long> recipeIds = recipeIdsAsString.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // DB에서 레시피 정보(Recipe 엔티티) 조회
        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
        Map<Long, Recipe> recipeMap = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        // DB에서 해당 user와 recipeIds에 대한 UserRecipe 정보 조회
        List<UserRecipe> userRecipes = userRecipeRepository.findByUserIdAndRecipeIdIn(userId, recipeIds);
        Map<Long, UserRecipe> userRecipeMap = userRecipes.stream()
                .collect(Collectors.toMap(
                        userRecipe -> userRecipe.getRecipe().getId(),
                        Function.identity(),
                        (existing, replacement) -> replacement
                ));

        return toRecentRecipeDTOList(recipeIds, recipeMap,userRecipeMap);

    }
    @Override
    public Long getMostRecentRecipeId(Long userId) {

        String key = "user:" + userId + ":recent_recipes";

        Set<String> recipeIdsAsString = redisTemplate.opsForZSet().reverseRange(key, 0, 0);

        if (recipeIdsAsString == null || recipeIdsAsString.isEmpty()) {
            throw new GlobalException(ErrorCode.RECENT_RECIPE_NOT_FOUND);
        }

        return Long.parseLong(recipeIdsAsString.iterator().next());
    }


}
