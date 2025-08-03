package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.converter.RecipeConverter;
import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;

@Service
public class RecipeServiceImpl implements RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeServiceImpl.class);
    private static final Duration RECIPE_CACHE_TTL = Duration.ofDays(7); // 7일 동안 캐시

    private final RecipeRepository recipeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecipeServiceImpl(RecipeRepository recipeRepository, RedisTemplate<String, Object> redisTemplate) {
        this.recipeRepository = recipeRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public void createRecipe(RecipeRequest.CreateReq request) {
        log.info("레시피 등록 시작: {}", request.getTitle());
        
        // 1. Redis로 중복 검증 (빠른 검증)
        log.info("Redis 중복 검증 시작: {}", request.getTitle());
        if (isRecipeTitleExistsInRedis(request.getTitle())) {
            log.warn("Redis에서 중복 레시피 발견: {}", request.getTitle());
            throw new BusinessException(ErrorCode.RECIPE_DUPLICATE_TITLE);
        }
        log.info("Redis 중복 검증 통과: {}", request.getTitle());
        
        // 2. DB로 중복 검증 (정확한 검증)
        log.info("DB 중복 검증 시작: {}", request.getTitle());
        if (recipeRepository.existsByTitle(request.getTitle())) {
            log.warn("DB에서 중복 레시피 발견: {}", request.getTitle());
            throw new BusinessException(ErrorCode.RECIPE_DUPLICATE_TITLE);
        }
        log.info("DB 중복 검증 통과: {}", request.getTitle());
        
        // 3. DB에 레시피 저장
        Recipe recipe = RecipeConverter.toRecipe(request);
        Recipe savedRecipe = recipeRepository.save(recipe);
        log.info("DB 저장 완료: {} (ID: {})", savedRecipe.getTitle(), savedRecipe.getId());
        
        // 4. DB 저장 성공 시 Redis에 완전한 캐싱
        cacheRecipeTitleToRedis(savedRecipe);
        log.info("레시피 등록 완료: {}", savedRecipe.getTitle());
    }

    @Override
    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
        }
        recipeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse.DetailRes getRecipeDetail(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        return RecipeConverter.toDetailRes(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findNamesByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        RecipeCategory recipeCategory;
        try {
            recipeCategory = RecipeCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        return recipeRepository.findNamesByCategories(List.of(recipeCategory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findNamesByCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        
        String[] categoryArray = categories.split(",");
        List<RecipeCategory> recipeCategories = new ArrayList<>();
        List<String> invalidCategories = new ArrayList<>();
        
        for (String category : categoryArray) {
            try {
                RecipeCategory recipeCategory = RecipeCategory.valueOf(category.trim().toUpperCase());
                recipeCategories.add(recipeCategory);
            } catch (IllegalArgumentException e) {
                invalidCategories.add(category.trim());
            }
        }
        
        if (!invalidCategories.isEmpty()) {
            log.warn("유효하지 않은 카테고리들이 무시되었습니다: {}", String.join(", ", invalidCategories));
        }
        
        if (recipeCategories.isEmpty()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        
        return recipeRepository.findNamesByCategories(recipeCategories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponse.DetailRes> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(RecipeConverter::toDetailRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponse.SimpleRes> getSimpleRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(RecipeConverter::toSimpleRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateRecipe(Long id, RecipeRequest.CreateReq request) {
        Recipe recipe = recipeRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));

        if (request.getTitle() != null) recipe.setTitle(request.getTitle());
        if (request.getRecipeImage() != null) recipe.setRecipeImage(request.getRecipeImage());
        if (request.getDescription() != null) recipe.setDescription(request.getDescription());
        if (request.getCookingTime() != null) recipe.setCookingTime(request.getCookingTime());
        if (request.getCalories() != null) recipe.setCalories(request.getCalories());
        if (request.getProtein() != null) recipe.setProtein(request.getProtein());
        if (request.getCarbohydrate() != null) recipe.setCarbohydrate(request.getCarbohydrate());
        if (request.getFat() != null) recipe.setFat(request.getFat());
        if (request.getIngredients() != null) recipe.setIngredients(request.getIngredients());

        if (request.getCategories() != null) {
            List<RecipeCategory> categoryList = new ArrayList<>();
            for (String categoryStr : request.getCategories()) {
                try {
                    categoryList.add(RecipeCategory.valueOf(categoryStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(ErrorCode.RECIPE_INVALID_CATEGORY);
                }
            }
            recipe.setCategories(categoryList);
        }

        recipeRepository.save(recipe);
    }

    /**
     * 레시피 제목을 Redis에 캐싱 (ZSet으로 통합)
     */
    private void cacheRecipeTitleToRedis(Recipe recipe) {
        try {
            log.debug("Redis 캐싱 시작: '{}'", recipe.getTitle());
            
            // ZSet 하나로 중복 방지 + 자동완성 모두 처리
            String titleToCache = recipe.getTitle().toLowerCase();
            log.debug("캐싱할 제목 (소문자): '{}'", titleToCache);
            
            Boolean result = redisTemplate.opsForZSet().add("recipetitles", titleToCache, 0);
            log.debug("Redis ZSet 추가 결과: '{}' -> 성공: {}", titleToCache, result);
            
            if (result != null && result) {
                log.info("레시피 제목 Redis 캐싱 성공: '{}' (ZSet 통합)", recipe.getTitle());
            } else {
                log.warn("레시피 제목 Redis 캐싱 실패 (이미 존재): '{}'", recipe.getTitle());
            }
        } catch (Exception e) {
            log.warn("레시피 제목 Redis 캐싱 실패: {} - {}", recipe.getTitle(), e.getMessage());
        }
    }

    /**
     * Redis ZSet에 해당 제목의 레시피가 존재하는지 확인합니다.
     */
    private boolean isRecipeTitleExistsInRedis(String title) {
        try {
            log.debug("Redis ZSet 검색 시작: '{}'", title);
            
            // ZSet에서 제목 검색 (소문자로 통일)
            String searchTitle = title.toLowerCase();
            log.debug("검색할 제목 (소문자): '{}'", searchTitle);
            
            Double score = redisTemplate.opsForZSet().score("recipetitles", searchTitle);
            boolean exists = score != null;
            
            log.debug("Redis 검색 결과: '{}' -> 존재: {}", searchTitle, exists);
            
            if (exists) {
                log.info("Redis에서 레시피 제목 발견: '{}' (점수: {})", searchTitle, score);
            } else {
                log.debug("Redis에서 레시피 제목 없음: '{}'", searchTitle);
            }
            
            return exists;
        } catch (Exception e) {
            log.warn("Redis 중복 검사 실패: {} - {}", title, e.getMessage());
            return false;
        }
    }
}
